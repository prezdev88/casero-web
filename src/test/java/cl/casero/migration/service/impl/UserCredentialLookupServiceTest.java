package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.PlatformTransactionManager;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.repository.AppUserRepository;
import cl.casero.migration.service.UserCredentialLookup;
import cl.casero.migration.service.dto.UserCredentials;
import cl.casero.migration.service.dto.UserIdentity;
import cl.casero.migration.util.PinHasher;
import cl.casero.migration.web.UserSessionAdvice;
import cl.casero.migration.web.UserSessionAdvice.CurrentUser;
import cl.casero.migration.web.security.CaseroUserDetails;
import cl.casero.migration.web.security.PinAuthenticationProvider;
import cl.casero.migration.web.security.PinAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserCredentialLookupServiceTest {

    private static final String PIN = "1234";
    private static final String WRONG_PIN = "9999";
    private static final String SALT = "test-salt";
    private static final long USER_ID = 7L;

    @Mock
    private AppUserRepository repository;

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager manager;
    private PinAuthenticationProvider provider;
    private PinHasher hasher;
    private String fingerprint;
    private AppUser user;

    @BeforeEach
    void setUp() {
        hasher = new PinHasher();
        fingerprint = hasher.fingerprint(PIN);
        String hash = hasher.hashWithSalt(PIN, SALT);
        user = new AppUser();
        user.setId(USER_ID);
        user.setName("Test User");
        user.setRole(UserRole.NORMAL);
        user.setPinFingerprint(fingerprint);
        user.setPinHash(hash);
        user.setPinSalt(SALT);
        manager = new RecordingTransactionManager();
        context = new AnnotationConfigApplicationContext();
        context.register(TransactionTestConfiguration.class);
        context.registerBean(AppUserRepository.class, () -> repository);
        context.registerBean(PlatformTransactionManager.class, () -> manager);
        context.registerBean(UserCredentialLookupService.class);
        context.refresh();
        UserCredentialLookup lookup = context.getBean(UserCredentialLookup.class);
        provider = new PinAuthenticationProvider(hasher, lookup);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void authenticatesThroughReadOnlyLookupAndKeepsRolesIdentityAndRequestDetails(UserRole role) {
        user.setRole(role);
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);
        request.setDetails("Request details");

        Authentication authentication = provider.authenticate(request);

        boolean authenticated = authentication.isAuthenticated();
        Object credentials = authentication.getCredentials();
        Object details = authentication.getDetails();
        CaseroUserDetails principal = (CaseroUserDetails) authentication.getPrincipal();
        UserIdentity actor = principal.getIdentity();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleName = role.name();
        String expectedAuthority = "ROLE_" + roleName;
        assertThat(authenticated).isTrue();
        assertThat(credentials).isNull();
        assertThat(details).isEqualTo("Request details");
        UserIdentity expectedIdentity = new UserIdentity(USER_ID, "Test User", role, true);
        assertThat(actor).isEqualTo(expectedIdentity);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly(expectedAuthority);
        UserSessionAdvice advice = new UserSessionAdvice();
        CurrentUser modelUser = advice.currentUser(authentication);
        boolean administrator = role == UserRole.ADMIN;
        CurrentUser expectedUser = new CurrentUser(USER_ID, "Test User", role, administrator);
        assertThat(modelUser).isEqualTo(expectedUser);
        assertReadOnlyLookup();
    }

    @Test
    void keepsBadCredentialsForAnAbsentUser() {
        Optional<AppUser> result = Optional.empty();
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);

        assertThatThrownBy(() -> provider.authenticate(request)).isInstanceOf(BadCredentialsException.class);
        assertReadOnlyLookup();
    }

    @Test
    void rejectsDisabledUsersBeforeGrantingAnAuthentication() {
        user.setEnabled(false);
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);

        assertThatThrownBy(() -> provider.authenticate(request)).isInstanceOf(DisabledException.class);
        assertReadOnlyLookup();
    }

    @Test
    void checksThePinHashAfterFindingTheUser() {
        String differentHash = hasher.hashWithSalt(WRONG_PIN, SALT);
        user.setPinHash(differentHash);
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);

        assertThatThrownBy(() -> provider.authenticate(request)).isInstanceOf(BadCredentialsException.class);
        assertReadOnlyLookup();
    }

    @Test
    void leavesOtherTokenTypesToTheirOwnProviders() {
        Authentication request = new TestingAuthenticationToken("other", "credentials");

        Authentication result = provider.authenticate(request);

        boolean supportsPin = provider.supports(PinAuthenticationToken.class);
        boolean supportsOther = provider.supports(TestingAuthenticationToken.class);
        assertThat(result).isNull();
        assertThat(supportsPin).isTrue();
        assertThat(supportsOther).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void keepsSessionIdentityStableAndRefreshesItOnANewAuthentication() {
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);
        Authentication authentication = provider.authenticate(request);
        CaseroUserDetails principal = (CaseroUserDetails) authentication.getPrincipal();

        user.setName("Changed User");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(false);

        UserIdentity identity = principal.getIdentity();
        UserIdentity expected = new UserIdentity(USER_ID, "Test User", UserRole.NORMAL, true);
        boolean enabled = principal.isEnabled();
        boolean administrator = principal.isAdmin();
        assertThat(identity).isEqualTo(expected);
        assertThat(enabled).isTrue();
        assertThat(administrator).isFalse();
        assertThatThrownBy(() -> provider.authenticate(request)).isInstanceOf(DisabledException.class);

        user.setEnabled(true);
        Authentication refreshed = provider.authenticate(request);
        CaseroUserDetails refreshedPrincipal = (CaseroUserDetails) refreshed.getPrincipal();
        UserIdentity refreshedIdentity = refreshedPrincipal.getIdentity();
        UserIdentity expectedRefreshed = new UserIdentity(USER_ID, "Changed User", UserRole.ADMIN, true);
        assertThat(refreshedIdentity).isEqualTo(expectedRefreshed);
    }

    @Test
    void serializesAuthenticatedSessionWithoutPersistentEntities() throws IOException, ClassNotFoundException {
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        PinAuthenticationToken request = new PinAuthenticationToken(PIN);
        Authentication authentication = provider.authenticate(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(output)) {
            stream.writeObject(authentication);
        }

        byte[] serialized = output.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(serialized);
        try (ObjectInputStream stream = new ObjectInputStream(input)) {
            Authentication restored = (Authentication) stream.readObject();
            CaseroUserDetails principal = (CaseroUserDetails) restored.getPrincipal();
            UserIdentity actual = principal.getIdentity();
            UserIdentity expected = new UserIdentity(USER_ID, "Test User", UserRole.NORMAL, true);
            String username = principal.getUsername();
            String password = principal.getPassword();
            String expectedHash = user.getPinHash();
            Object credentials = restored.getCredentials();
            assertThat(actual).isEqualTo(expected);
            assertThat(username).isEqualTo(fingerprint);
            assertThat(password).isEqualTo(expectedHash);
            assertThat(credentials).isNull();
        }
    }

    @Test
    void copiesCredentialValuesAndOmitsThemFromDiagnostics() {
        Optional<AppUser> result = Optional.of(user);
        doReturn(result).when(repository).findByPinFingerprint(fingerprint);
        UserCredentialLookup lookup = context.getBean(UserCredentialLookup.class);
        UserCredentials credentials = lookup.findByPinFingerprint(fingerprint).orElseThrow();
        String expectedHash = user.getPinHash();

        user.setPinHash("Changed hash");
        user.setPinSalt("Changed salt");
        user.setPinFingerprint("Changed fingerprint");

        String hash = credentials.pinHash();
        String salt = credentials.pinSalt();
        String storedFingerprint = credentials.pinFingerprint();
        String diagnostic = credentials.toString();
        assertThat(hash).isEqualTo(expectedHash);
        assertThat(salt).isEqualTo(SALT);
        assertThat(storedFingerprint).isEqualTo(fingerprint);
        assertThat(diagnostic).doesNotContain(expectedHash, SALT, fingerprint);
        assertReadOnlyLookup();
    }

    private void assertReadOnlyLookup() {
        boolean readOnly = manager.isReadOnly();
        int commits = manager.getCommitCount();
        int begins = manager.getBeginCount();
        assertThat(readOnly).isTrue();
        assertThat(commits).isEqualTo(1);
        assertThat(begins).isEqualTo(1);
    }
}
