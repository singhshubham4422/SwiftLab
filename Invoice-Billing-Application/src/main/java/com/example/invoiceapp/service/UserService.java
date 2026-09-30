package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.UserDTO;
import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.model.enums.UserRole;
import com.example.invoiceapp.repository.OrganizationRepository;
import com.example.invoiceapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CompanySettingsService settingsService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       CompanySettingsService settingsService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.settingsService = settingsService;
        this.passwordEncoder = passwordEncoder;
    }

    public long countUsers() {
        return userRepository.count();
    }

    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public List<UserDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        return userRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public User register(String email,
                         String password,
                         String fullName,
                         String organizationName,
                         String currencySymbol,
                         String currencyCode,
                         String tagline,
                         String phone,
                         String address) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        // 1. Create Organization
        Organization org = new Organization();
        org.setName(organizationName != null && !organizationName.isBlank() ? organizationName.trim() : "Default Organization");
        org.setPreferredDataMode(DataMode.LOCAL_ONLY);
        Organization savedOrg = organizationRepository.save(org);

        // 2. Create User as OWNER with BCrypt encoded password
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(cleanEmail, encodedPassword, fullName.trim(), savedOrg.getName());
        user.setOrganizationId(savedOrg.getId());
        user.setRole(UserRole.OWNER);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // 3. Configure user's organization settings
        CompanySettings settings = settingsService.getSettingsForUser(savedUser.getId());
        settings.setUserId(savedUser.getId());
        settings.setOrganizationId(savedOrg.getId());
        settings.setUserName(fullName.trim());
        settings.setOrganizationName(savedOrg.getName());
        if (tagline != null && !tagline.trim().isEmpty()) {
            settings.setTagline(tagline.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            settings.setPhone(phone.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            settings.setAddress(address.trim());
        }
        settings.setEmail(cleanEmail);
        settings.setCurrencySymbol(currencySymbol != null ? currencySymbol : "₹");
        settings.setCurrencyCode(currencyCode != null ? currencyCode : "INR");
        settings.setAutoSync(false); // LOCAL_ONLY is strictly default!
        settings.setConfigured(true);
        settingsService.saveSettings(settings);

        return savedUser;
    }

    @Transactional
    public User createOrgUser(Long organizationId, String email, String password, String fullName, UserRole role) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        User user = new User(cleanEmail, passwordEncoder.encode(password), fullName.trim(), org.getName());
        user.setOrganizationId(organizationId);
        user.setRole(role != null ? role : UserRole.STAFF);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                return Optional.empty();
            }
            String stored = user.getPassword();
            if (isBcrypt(stored)) {
                if (passwordEncoder.matches(rawPassword, stored)) {
                    user.setLastLoginAt(Instant.now());
                    userRepository.save(user);
                    return Optional.of(user);
                }
            } else {
                // Fallback to legacy SHA-256 and upgrade password to BCrypt
                if (legacySha256(rawPassword).equalsIgnoreCase(stored)) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setLastLoginAt(Instant.now());
                    userRepository.save(user);
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    @Transactional
    public User updateRole(Long userId, Long organizationId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getOrganizationId() != null && u.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("User not found in organization"));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public User toggleActive(Long userId, Long organizationId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getOrganizationId() != null && u.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("User not found in organization"));
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    public UserDTO toDTO(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getOrganizationName(),
                user.getOrganizationId(),
                user.getRole(),
                user.isActive()
        );
    }

    private static boolean isBcrypt(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    public static String legacySha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
