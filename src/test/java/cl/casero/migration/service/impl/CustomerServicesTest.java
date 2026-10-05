package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.CustomerRepository.OverdueCustomerView;
import cl.casero.migration.service.CustomerCommands;
import cl.casero.migration.service.CustomerNotFoundException;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.CustomerBirthdayDTO;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.SectorSummary;
import cl.casero.migration.support.ReadModelFixtures;

@ExtendWith(MockitoExtension.class)
class CustomerServicesTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long SECTOR_ID = 3L;
    private static final int EXISTING_DEBT = 200;
    private static final int PAGE_SIZE = 2;
    private static final long TOTAL_CUSTOMERS = 5L;
    private static final long OVERDUE_DEBT = 500L;
    private static final int BIRTH_DAY = 4;
    private static final int BIRTH_MONTH = 10;
    private static final int BIRTH_YEAR = 1990;

    @Mock
    private CustomerRepository repository;

    @Mock
    private SectorService sectors;

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager manager;
    private CustomerCommands commands;
    private CustomerQueries queries;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("Original Name");
        customer.setAddress("Original Address");
        customer.setDebt(EXISTING_DEBT);
        manager = new RecordingTransactionManager();
        context = new AnnotationConfigApplicationContext();
        context.register(TransactionTestConfiguration.class);
        context.registerBean(CustomerRepository.class, () -> repository);
        context.registerBean(SectorService.class, () -> sectors);
        context.registerBean(PlatformTransactionManager.class, () -> manager);
        context.registerBean(CustomerCommandService.class);
        context.registerBean(CustomerQueryService.class);
        context.refresh();
        commands = context.getBean(CustomerCommands.class);
        queries = context.getBean(CustomerQueries.class);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void createsEnabledCustomersWithTrimmedFieldsAndZeroDebtInAWriteTransaction() {
        trackWrites();
        Sector sector = new Sector();
        sector.setId(SECTOR_ID);
        doReturn(sector).when(sectors).get(SECTOR_ID);
        CreateCustomerForm form = new CreateCustomerForm();
        form.setName("  New Name  ");
        form.setAddress("  New Address  ");
        form.setSectorId(SECTOR_ID);

        Customer created = commands.create(form);

        Customer saved = savedCustomer();
        String name = saved.getName();
        String address = saved.getAddress();
        Sector savedSector = saved.getSector();
        Integer debt = saved.getDebt();
        boolean enabled = saved.isEnabled();
        assertThat(created).isSameAs(saved);
        assertThat(name).isEqualTo("New Name");
        assertThat(address).isEqualTo("New Address");
        assertThat(savedSector).isSameAs(sector);
        assertThat(debt).isZero();
        assertThat(enabled).isTrue();
        assertCommittedWrite();
    }

    @Test
    void softDeletesWithoutChangingDebtOrDeletingTheEntity() {
        prepareExistingCustomer();
        trackWrites();

        commands.delete(CUSTOMER_ID);

        Customer saved = savedCustomer();
        boolean enabled = saved.isEnabled();
        Integer debt = saved.getDebt();
        assertThat(saved).isSameAs(customer);
        assertThat(enabled).isFalse();
        assertThat(debt).isEqualTo(EXISTING_DEBT);
        verify(repository).findByIdAndEnabledTrue(CUSTOMER_ID);
        verifyNoMoreInteractions(repository);
        assertCommittedWrite();
    }

    @Test
    void preservesAddressWhitespaceAndOtherFields() {
        prepareExistingCustomer();
        trackWrites();

        commands.updateAddress(CUSTOMER_ID, "  New\n Address  ");

        Customer saved = savedCustomer();
        String address = saved.getAddress();
        String name = saved.getName();
        Integer debt = saved.getDebt();
        assertThat(address).isEqualTo("  New\n Address  ");
        assertThat(name).isEqualTo("Original Name");
        assertThat(debt).isEqualTo(EXISTING_DEBT);
        assertCommittedWrite();
    }

    @Test
    void trimsUpdatedNamesWithoutChangingTheAddress() {
        prepareExistingCustomer();
        trackWrites();

        commands.updateName(CUSTOMER_ID, "  New Name  ");

        Customer saved = savedCustomer();
        String name = saved.getName();
        String address = saved.getAddress();
        assertThat(name).isEqualTo("New Name");
        assertThat(address).isEqualTo("Original Address");
        assertCommittedWrite();
    }

    @Test
    void changesTheSectorInsideTheCustomerWriteTransaction() {
        prepareExistingCustomer();
        trackWrites();
        Sector sector = new Sector();
        sector.setId(SECTOR_ID);
        doReturn(sector).when(sectors).get(SECTOR_ID);

        commands.updateSector(CUSTOMER_ID, SECTOR_ID);

        Customer saved = savedCustomer();
        Sector savedSector = saved.getSector();
        assertThat(savedSector).isSameAs(sector);
        assertCommittedWrite();
    }

    @ParameterizedTest
    @CsvSource({"4, 10, 1990", "4, 10,", ",,"})
    void preservesOptionalBirthdateFieldsAndClearing(Integer day, Integer month, Integer year) {
        prepareExistingCustomer();
        trackWrites();
        customer.setBirthDay(BIRTH_DAY);
        customer.setBirthMonth(BIRTH_MONTH);
        customer.setBirthYear(BIRTH_YEAR);

        commands.updateBirthdate(CUSTOMER_ID, day, month, year);

        Customer saved = savedCustomer();
        Integer savedDay = saved.getBirthDay();
        Integer savedMonth = saved.getBirthMonth();
        Integer savedYear = saved.getBirthYear();
        assertThat(savedDay).isEqualTo(day);
        assertThat(savedMonth).isEqualTo(month);
        assertThat(savedYear).isEqualTo(year);
        assertCommittedWrite();
    }

    @Test
    void rejectsMissingOrDisabledCustomersInCommandsBeforeSaving() {
        Optional<Customer> absent = Optional.empty();
        doReturn(absent).when(repository).findByIdAndEnabledTrue(CUSTOMER_ID);

        assertThatThrownBy(() -> commands.updateAddress(CUSTOMER_ID, "New Address"))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(repository).findByIdAndEnabledTrue(CUSTOMER_ID);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(sectors);
    }

    @Test
    void doesNotSaveWhenTheNewSectorCannotBeResolved() {
        prepareExistingCustomer();
        IllegalArgumentException failure = new IllegalArgumentException("Sector unavailable");
        doThrow(failure).when(sectors).get(SECTOR_ID);

        assertThatThrownBy(() -> commands.updateSector(CUSTOMER_ID, SECTOR_ID)).isSameAs(failure);

        verify(repository).findByIdAndEnabledTrue(CUSTOMER_ID);
        verifyNoMoreInteractions(repository);
        int rollbacks = manager.getRollbackCount();
        assertThat(rollbacks).isEqualTo(1);
    }

    @Test
    void requestsRollbackWhenSavingTheCustomerFails() {
        prepareExistingCustomer();
        IllegalStateException failure = new IllegalStateException("Customer persistence failed");
        Answer<Customer> answer = invocation -> {
            assertWriteIsActive();
            throw failure;
        };
        CustomerRepository stub = doAnswer(answer).when(repository);
        Customer matchedCustomer = any(Customer.class);
        stub.save(matchedCustomer);

        assertThatThrownBy(() -> commands.delete(CUSTOMER_ID)).isSameAs(failure);

        int commits = manager.getCommitCount();
        int rollbacks = manager.getRollbackCount();
        assertThat(commits).isZero();
        assertThat(rollbacks).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void keepsBlankSearchesEmptyWithoutQueryingTheRepository(String filter) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        Page<CustomerDetails> result = queries.search(filter, pageable);

        List<CustomerDetails> content = result.getContent();
        Pageable actualPageable = result.getPageable();
        assertThat(content).isEmpty();
        assertThat(actualPageable).isEqualTo(pageable);
        verifyNoInteractions(repository, sectors);
    }

    @Test
    void preservesSearchTrimmingAndPagination() {
        Pageable pageable = PageRequest.of(1, PAGE_SIZE);
        List<Customer> content = List.of(customer);
        Page<Customer> page = new PageImpl<>(content, pageable, TOTAL_CUSTOMERS);
        doReturn(page).when(repository).search("Test", pageable);

        Page<CustomerDetails> result = queries.search("  Test  ", pageable);

        List<CustomerDetails> expected = ReadModelFixtures.customers(content);
        List<CustomerDetails> actual = result.getContent();
        Pageable actualPage = result.getPageable();
        long total = result.getTotalElements();
        assertThat(actual).containsExactlyElementsOf(expected);
        assertThat(actualPage).isEqualTo(pageable);
        assertThat(total).isEqualTo(TOTAL_CUSTOMERS);
    }

    @Test
    void rejectsMissingOrDisabledCustomersInQueries() {
        Optional<Customer> absent = Optional.empty();
        doReturn(absent).when(repository).findByIdAndEnabledTrue(CUSTOMER_ID);

        assertThatThrownBy(() -> queries.get(CUSTOMER_ID)).isInstanceOf(CustomerNotFoundException.class);

        verify(repository).findByIdAndEnabledTrue(CUSTOMER_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void resolvesQueriesSeparatelyAndExecutesReadsInAReadOnlyTransaction() {
        Optional<Customer> existing = Optional.of(customer);
        Answer<Optional<Customer>> answer = invocation -> {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            assertThat(active).isTrue();
            assertThat(readOnly).isTrue();
            return existing;
        };
        doAnswer(answer).when(repository).findByIdAndEnabledTrue(CUSTOMER_ID);

        CustomerDetails result = queries.get(CUSTOMER_ID);

        CustomerDetails expected = ReadModelFixtures.customer(customer);
        assertThat(result).isEqualTo(expected);
        assertThat(queries).isNotSameAs(commands);
        int commits = manager.getCommitCount();
        boolean readOnly = manager.isReadOnly();
        assertThat(commits).isEqualTo(1);
        assertThat(readOnly).isTrue();
        verifyNoInteractions(sectors);
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 3})
    void preservesOverdueMonthBoundsAndNullableProjectionFields(int months) {
        int expectedMonths = Math.max(months, 1);
        Pageable pageable = PageRequest.of(1, PAGE_SIZE);
        OverdueCustomerView view = new OverdueRow(CUSTOMER_ID, "Test Customer", "Central", EXISTING_DEBT, "Never paid", null);
        List<OverdueCustomerView> content = List.of(view);
        Page<OverdueCustomerView> page = new PageImpl<>(content, pageable, TOTAL_CUSTOMERS);
        doReturn(page).when(repository).findOverdueCustomers(pageable, expectedMonths);
        doReturn(OVERDUE_DEBT).when(repository).sumOverdueDebt(expectedMonths);

        Page<OverdueCustomerSummary> result = queries.getOverdueCustomers(pageable, months);
        long overdueDebt = queries.getOverdueDebt(months);

        List<OverdueCustomerSummary> summaries = result.getContent();
        OverdueCustomerSummary expected = new OverdueCustomerSummary(CUSTOMER_ID, "Test Customer", "Central", EXISTING_DEBT, "Never paid", null);
        long total = result.getTotalElements();
        Pageable actualPageable = result.getPageable();
        assertThat(summaries).containsExactly(expected);
        assertThat(total).isEqualTo(TOTAL_CUSTOMERS);
        assertThat(actualPageable).isEqualTo(pageable);
        assertThat(overdueDebt).isEqualTo(OVERDUE_DEBT);
    }

    @Test
    void preservesDebtOrderingQueries() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        List<Customer> content = List.of(customer);
        Page<Customer> page = new PageImpl<>(content, pageable, 1);
        doReturn(page).when(repository).findAllByEnabledTrueOrderByDebtDesc(pageable);
        doReturn(page).when(repository).findAllByEnabledTrueOrderByDebtAsc(pageable);

        Page<CustomerDetails> topDebtors = queries.getTopDebtors(pageable);
        Page<CustomerDetails> bestCustomers = queries.getBestCustomers(pageable);

        List<CustomerDetails> expected = ReadModelFixtures.customers(content);
        List<CustomerDetails> top = topDebtors.getContent();
        List<CustomerDetails> best = bestCustomers.getContent();
        assertThat(top).containsExactlyElementsOf(expected);
        assertThat(best).containsExactlyElementsOf(expected);
    }

    @Test
    void preservesActiveCustomerCountsAndBirthdayResults() {
        CustomerBirthdayDTO birthday = new CustomerBirthdayDTO(CUSTOMER_ID, "Test Customer", BIRTH_DAY, BIRTH_MONTH, null, EXISTING_DEBT, null);
        List<CustomerBirthdayDTO> birthdays = List.of(birthday);
        doReturn(TOTAL_CUSTOMERS).when(repository).countByEnabledTrue();
        doReturn(1L).when(repository).countBirthdaysThisMonth(BIRTH_MONTH);
        doReturn(birthdays).when(repository).findBirthdaysThisMonth(BIRTH_MONTH);

        long count = queries.count();
        long birthdayCount = queries.getBirthdaysThisMonthCount(BIRTH_MONTH);
        List<CustomerBirthdayDTO> result = queries.getBirthdaysThisMonth(BIRTH_MONTH);

        assertThat(count).isEqualTo(TOTAL_CUSTOMERS);
        assertThat(birthdayCount).isEqualTo(1);
        assertThat(result).isSameAs(birthdays);
    }

    @Test
    void returnsCustomerValuesIndependentOfEntityAndSectorChanges() {
        Sector sector = new Sector();
        sector.setId(SECTOR_ID);
        sector.setName("Original Sector");
        customer.setSector(sector);
        customer.setBirthDay(BIRTH_DAY);
        customer.setBirthMonth(BIRTH_MONTH);
        customer.setBirthYear(BIRTH_YEAR);
        prepareExistingCustomer();

        CustomerDetails result = queries.get(CUSTOMER_ID);

        customer.setName("Changed Name");
        customer.setAddress("Changed Address");
        customer.setDebt(0);
        customer.setBirthYear(null);
        sector.setName("Changed Sector");
        SectorSummary originalSector = new SectorSummary(SECTOR_ID, "Original Sector");
        CustomerDetails expected = new CustomerDetails(CUSTOMER_ID, "Original Name", "Original Address",
                EXISTING_DEBT, originalSector, BIRTH_DAY, BIRTH_MONTH, BIRTH_YEAR);
        assertThat(result).isEqualTo(expected);
    }

    private void prepareExistingCustomer() {
        Optional<Customer> existing = Optional.of(customer);
        doReturn(existing).when(repository).findByIdAndEnabledTrue(CUSTOMER_ID);
    }

    private void trackWrites() {
        Answer<Customer> answer = invocation -> {
            assertWriteIsActive();
            return invocation.getArgument(0);
        };
        CustomerRepository stub = doAnswer(answer).when(repository);
        Customer matchedCustomer = any(Customer.class);
        stub.save(matchedCustomer);
    }

    private void assertWriteIsActive() {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        assertThat(active).isTrue();
        assertThat(readOnly).isFalse();
    }

    private Customer savedCustomer() {
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        CustomerRepository verification = verify(repository);
        Customer captured = captor.capture();
        verification.save(captured);
        return captor.getValue();
    }

    private void assertCommittedWrite() {
        int begins = manager.getBeginCount();
        int commits = manager.getCommitCount();
        boolean readOnly = manager.isReadOnly();
        assertThat(begins).isEqualTo(1);
        assertThat(commits).isEqualTo(1);
        assertThat(readOnly).isFalse();
    }

    private record OverdueRow(Long id, String name, String sector, Integer debt, String lastPayment, Integer monthsOverdue)
            implements OverdueCustomerView {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getSector() {
            return sector;
        }

        @Override
        public Integer getDebt() {
            return debt;
        }

        @Override
        public String getLast_payment() {
            return lastPayment;
        }

        @Override
        public Integer getMonths_overdue() {
            return monthsOverdue;
        }
    }
}
