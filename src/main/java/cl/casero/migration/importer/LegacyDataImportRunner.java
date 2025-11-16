package cl.casero.migration.importer;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Statistic;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.SectorRepository;
import cl.casero.migration.domain.Sector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class LegacyDataImportRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyDataImportRunner.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");

    private final CustomerRepository customerRepository;
    private final SectorRepository sectorRepository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final String sqlitePath;

    public LegacyDataImportRunner(CustomerRepository customerRepository,
                                  SectorRepository sectorRepository,
                                  JdbcTemplate jdbcTemplate,
                                  @Value("${casero.import.enabled:true}") boolean enabled,
                                  @Value("${casero.import.sqlite-path:casero.sqlite}") String sqlitePath) {
        this.customerRepository = customerRepository;
        this.sectorRepository = sectorRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.sqlitePath = sqlitePath;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOGGER.info("Legacy data import disabled");
            return;
        }

        if (customerRepository.count() > 0) {
            LOGGER.info("Skipping legacy data import because PostgreSQL already has customers");
            return;
        }

        Path path = Path.of(sqlitePath);
        if (!Files.exists(path)) {
            LOGGER.warn("Legacy SQLite file not found at {}. Skipping import.", path.toAbsolutePath());
            return;
        }

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        try (Connection connection = config.createConnection("jdbc:sqlite:" + path.toAbsolutePath())) {

            Map<Long, Customer> customers = importCustomers(connection);
            Map<Long, Transaction> transactions = importTransactions(connection, customers);
            int statsCount = importStatistics(connection);

            resetSequences();

            LOGGER.info("Imported {} customers, {} transactions and {} statistics from {}",
                    customers.size(), transactions.size(), statsCount, path.toAbsolutePath());
        } catch (SQLException e) {
            LOGGER.error("Error importing legacy data from {}", path.toAbsolutePath(), e);
        }
    }

    private Map<Long, Customer> importCustomers(Connection connection) throws SQLException {
        Map<Long, Customer> customers = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, nombre, sector, direccion, deuda FROM cliente ORDER BY id")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                Customer customer = new Customer();
                customer.setId(id);
                customer.setName(trim(rs.getString("nombre")));
                customer.setAddress(trim(rs.getString("direccion")));
                customer.setDebt(rs.getInt("deuda"));
                customer.setSector(resolveSector(rs.getString("sector")));
                customers.put(id, customer);
            }
        }

        batchInsertCustomers(new ArrayList<>(customers.values()));
        return customers;
    }

    private Map<Long, Transaction> importTransactions(Connection connection, Map<Long, Customer> customers) throws SQLException {
        Map<Long, Transaction> transactions = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, cliente, fecha, detalle, amount, saldo, type FROM movimiento ORDER BY id")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                long customerId = rs.getLong("cliente");
                Customer customer = customers.get(customerId);
                if (customer == null) {
                    customer = new Customer();
                    customer.setId(customerId);
                    customer.setName("Cliente " + customerId + " (migrado)");
                    customer.setAddress("Dirección desconocida");
                    customer.setDebt(0);
                    customer.setSector(resolveSector("Sin sector"));
                    customerRepository.save(customer);
                    customers.put(customerId, customer);
                }

                TransactionType type;
                try {
                    type = TransactionType.fromLegacyId(rs.getInt("type"));
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Skipping transaction {} due to unsupported type: {}", rs.getLong("id"), rs.getInt("type"));
                    continue;
                }

                Transaction transaction = new Transaction();
                transaction.setId(rs.getLong("id"));
                transaction.setCustomer(customer);
                transaction.setDate(parseDate(rs.getString("fecha")));
                transaction.setDetail(trim(rs.getString("detalle")));
                transaction.setAmount(rs.getInt("amount"));
                transaction.setBalance(rs.getInt("saldo"));
                transaction.setType(type);
                LocalDate date = transaction.getDate();
                OffsetDateTime createdAt = date != null
                        ? date.atStartOfDay(DEFAULT_ZONE).toOffsetDateTime()
                        : OffsetDateTime.now(DEFAULT_ZONE);
                transaction.setCreatedAt(createdAt);

                transactions.put(transaction.getId(), transaction);
            }
        }

        batchInsertTransactions(new ArrayList<>(transactions.values()));
        return transactions;
    }

    private int importStatistics(Connection connection) throws SQLException {
        List<Statistic> statistics = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, tipo, monto, fecha, tipoVenta, cantPrendas FROM estadistica ORDER BY id")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Statistic statistic = new Statistic();
                statistic.setId(rs.getLong("id"));

                try {
                    statistic.setType(TransactionType.fromLegacyId(rs.getInt("tipo")));
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Skipping statistic {} due to unsupported type {}", statistic.getId(), rs.getInt("tipo"));
                    continue;
                }

                statistic.setAmount(rs.getInt("monto"));
                statistic.setDate(parseDate(rs.getString("fecha")));

                int saleTypeId = rs.getInt("tipoVenta");
                if (!rs.wasNull() && saleTypeId >= 0) {
                    try {
                        statistic.setSaleType(SaleType.fromLegacyId(saleTypeId));
                    } catch (IllegalArgumentException ex) {
                        LOGGER.warn("Statistic {} has unsupported sale type {}", statistic.getId(), saleTypeId);
                    }
                }

                int itemsCount = rs.getInt("cantPrendas");
                if (!rs.wasNull() && itemsCount >= 0) {
                    statistic.setItemsCount(itemsCount);
                }

                statistics.add(statistic);
            }
        }

        batchInsertStatistics(statistics);
        return statistics.size();
    }

    private void batchInsertCustomers(Collection<Customer> customers) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO customer (id, name, sector_id, address, debt) VALUES (?, ?, ?, ?, ?)",
                customers,
                100,
                (ps, customer) -> {
                    ps.setLong(1, customer.getId());
                    ps.setString(2, customer.getName());
                    ps.setLong(3, customer.getSector().getId());
                    ps.setString(4, customer.getAddress());
                    ps.setInt(5, customer.getDebt());
                }
        );
    }

    private void batchInsertTransactions(Collection<Transaction> transactions) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO transaction (id, customer_id, date, detail, amount, balance, type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                transactions,
                100,
                (ps, transaction) -> {
                    ps.setLong(1, transaction.getId());
                    ps.setLong(2, transaction.getCustomer().getId());
                    ps.setObject(3, transaction.getDate());
                    ps.setString(4, transaction.getDetail());
                    ps.setInt(5, transaction.getAmount());
                    ps.setInt(6, transaction.getBalance());
                    ps.setString(7, transaction.getType().name());
                    ps.setObject(8, transaction.getCreatedAt());
                }
        );
    }

    private void batchInsertStatistics(Collection<Statistic> statistics) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO statistic (id, type, amount, sale_type, items_count, date) VALUES (?, ?, ?, ?, ?, ?)",
                statistics,
                100,
                (ps, statistic) -> {
                    ps.setLong(1, statistic.getId());
                    ps.setString(2, statistic.getType().name());
                    ps.setInt(3, statistic.getAmount());
                    if (statistic.getSaleType() != null) {
                        ps.setString(4, statistic.getSaleType().name());
                    } else {
                        ps.setNull(4, java.sql.Types.VARCHAR);
                    }
                    if (statistic.getItemsCount() != null) {
                        ps.setInt(5, statistic.getItemsCount());
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }
                    ps.setObject(6, statistic.getDate());
                }
        );
    }

    private Sector resolveSector(String sectorName) {
        String sanitized = trim(sectorName);
        if (sanitized.isBlank()) {
            sanitized = "Sin sector";
        }

        String finalName = sanitized;
        return sectorRepository.findByNameIgnoreCase(finalName)
                .orElseGet(() -> {
                    Sector sector = new Sector();
                    sector.setName(finalName);
                    return sectorRepository.save(sector);
                });
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            LOGGER.warn("Could not parse date '{}', falling back to today", value);
            return LocalDate.now();
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void resetSequences() {
        resetSequence("customer_id_seq", "customer");
        resetSequence("transaction_id_seq", "transaction");
        resetSequence("statistic_id_seq", "statistic");
    }

    private void resetSequence(String sequenceName, String tableName) {
        String sql = String.format(
                Locale.ROOT,
                "SELECT setval('%s', COALESCE((SELECT MAX(id) FROM %s), 0), true)",
                sequenceName,
                tableName
        );
        jdbcTemplate.execute(sql);
    }
}
