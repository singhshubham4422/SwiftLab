package com.example.invoiceapp;

import com.example.invoiceapp.model.ApiToken;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.UserRole;
import com.example.invoiceapp.repository.UserRepository;
import com.example.invoiceapp.service.ApiTokenService;
import com.example.invoiceapp.service.OrganizationService;
import com.example.invoiceapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthAndSecurityTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("User creation uses BCrypt and provisions organization")
    void testUserCreationBCrypt() {
        User user = userService.register("alice@test.com", "secretPassword123", "Alice Smith", "Alice Corp", "$", "USD", "", "", "");

        assertNotNull(user.getId());
        assertNotNull(user.getOrganizationId());
        assertTrue(user.getPassword().startsWith("$2a$"), "Password should be BCrypt hashed");
        assertTrue(passwordEncoder.matches("secretPassword123", user.getPassword()));

        Organization org = organizationService.get(user.getOrganizationId());
        assertNotNull(org);
        assertEquals("Alice Corp", org.getName());
    }

    @Test
    @DisplayName("Legacy SHA-256 password automatically upgrades to BCrypt upon successful login")
    void testLegacySha256AutoUpgrade() throws Exception {
        // Manually create legacy SHA-256 user
        String rawPassword = "oldLegacyPassword";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(rawPassword.getBytes());
        String sha256Hash = HexFormat.of().formatHex(hash);

        User legacyUser = new User();
        legacyUser.setEmail("legacy_bob@test.com");
        legacyUser.setPassword(sha256Hash);
        legacyUser.setFullName("Legacy Bob");
        legacyUser.setRole(UserRole.ADMIN);
        legacyUser.setActive(true);
        legacyUser = userRepository.save(legacyUser);

        // Authenticate via UserService
        Optional<User> authenticated = userService.authenticate("legacy_bob@test.com", rawPassword);
        assertTrue(authenticated.isPresent(), "Authentication should succeed for legacy SHA-256 hash");

        // Verify password in DB was upgraded to BCrypt
        User updated = userRepository.findById(legacyUser.getId()).orElseThrow();
        assertTrue(updated.getPassword().startsWith("$2a$"), "Password should be upgraded to BCrypt");
        assertTrue(passwordEncoder.matches(rawPassword, updated.getPassword()));
    }

    @Test
    @DisplayName("API Token issuance, validation, and revocation")
    void testApiTokenLifecycle() {
        User user = userService.register("token_user@test.com", "pass123", "Token User", "Token Org", "$", "USD", "", "", "");
        
        ApiToken token = apiTokenService.createToken(user);
        assertNotNull(token);
        assertNotNull(token.getToken());

        // Validate token
        Optional<ApiToken> validatedToken = apiTokenService.validateToken(token.getToken());
        assertTrue(validatedToken.isPresent());
        assertEquals(user.getId(), validatedToken.get().getUserId());
        assertEquals(user.getOrganizationId(), validatedToken.get().getOrganizationId());

        // Revoke token
        apiTokenService.revokeToken(token.getToken());
        Optional<ApiToken> revokedLookup = apiTokenService.validateToken(token.getToken());
        assertTrue(revokedLookup.isEmpty(), "Revoked token must not be valid");
    }

    @Test
    @DisplayName("Focused Test: Register organization -> create credentials -> login with exact same credentials -> successful authentication")
    void testRegistrationAndImmediateLoginFlow() {
        String testEmail = "newowner@enterpriseco.com";
        String testPassword = "MySecurePassword2026!";
        String orgName = "Enterprise Logistics Inc";
        String fullName = "Enterprise Admin";

        // 1. Register organization and user
        User registeredUser = userService.register(
                testEmail,
                testPassword,
                fullName,
                orgName,
                "₹",
                "INR",
                "Leading logistics provider",
                "+91 9876543210",
                "100 Logistics Way, Tech Park"
        );

        assertNotNull(registeredUser.getId(), "User ID should be generated");
        assertNotNull(registeredUser.getOrganizationId(), "Organization ID should be generated");
        assertNotNull(registeredUser.getUsername(), "Username must not be null (PostgreSQL constraint)");
        assertEquals("newowner@enterpriseco.com", registeredUser.getUsername());
        assertEquals("newowner@enterpriseco.com", registeredUser.getEmail());
        assertTrue(registeredUser.isActive(), "User must be active");
        assertEquals(UserRole.OWNER, registeredUser.getRole());

        // 2. Immediate login with the exact same credentials
        Optional<User> authResult = userService.authenticate(testEmail, testPassword);
        assertTrue(authResult.isPresent(), "Authentication must succeed with the exact registration credentials");

        User authenticatedUser = authResult.get();
        assertEquals(registeredUser.getId(), authenticatedUser.getId());
        assertEquals(registeredUser.getOrganizationId(), authenticatedUser.getOrganizationId());
        assertNotNull(authenticatedUser.getLastLoginAt(), "Last login timestamp should be recorded");

        // 3. Login with mixed case email and whitespace
        Optional<User> authMixedCase = userService.authenticate("  NEWOWNER@ENTERPRISECO.COM  ", testPassword);
        assertTrue(authMixedCase.isPresent(), "Authentication must succeed with case-insensitive / trimmed email");

        // 4. Login with username
        Optional<User> authUsername = userService.authenticate(registeredUser.getUsername(), testPassword);
        assertTrue(authUsername.isPresent(), "Authentication must succeed with username");

        // 5. Verification that wrong password fails
        Optional<User> authWrongPass = userService.authenticate(testEmail, "WrongPassword!");
        assertTrue(authWrongPass.isEmpty(), "Authentication must fail for incorrect password");
    }
}
