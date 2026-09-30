package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.AuthRequest;
import com.example.invoiceapp.dto.AuthResponse;
import com.example.invoiceapp.dto.OrganizationDTO;
import com.example.invoiceapp.dto.UserDTO;
import com.example.invoiceapp.model.ApiToken;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.ApiTokenService;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.OrganizationService;
import com.example.invoiceapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and token management APIs")
public class AuthApiController {

    private final UserService userService;
    private final OrganizationService orgService;
    private final ApiTokenService tokenService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public AuthApiController(UserService userService,
                           OrganizationService orgService,
                           ApiTokenService tokenService,
                           AuditLogService auditLogService,
                           DeviceIdentityService deviceIdentityService) {
        this.userService = userService;
        this.orgService = orgService;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain bearer token")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        Optional<User> userOpt = userService.authenticate(req.getEmail(), req.getPassword());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
        User user = userOpt.get();
        ApiToken token = tokenService.createToken(user);
        Organization org = user.getOrganizationId() != null ? orgService.findById(user.getOrganizationId()).orElse(null) : null;

        auditLogService.record(
                user.getOrganizationId(),
                user.getId(),
                deviceIdentityService.localDeviceId(),
                "API_LOGIN",
                "User",
                user.getId().toString(),
                "REST API authentication success"
        );

        UserDTO userDTO = userService.toDTO(user);
        OrganizationDTO orgDTO = orgService.toDTO(org);
        return ResponseEntity.ok(new AuthResponse(token.getToken(), userDTO, orgDTO));
    }

    @PostMapping("/signup")
    @Operation(summary = "Register new organization owner")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String organizationName = body.get("organizationName");
        String currencySymbol = body.getOrDefault("currencySymbol", "₹");
        String currencyCode = body.getOrDefault("currencyCode", "INR");

        if (email == null || password == null || fullName == null || organizationName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email, password, fullName, and organizationName are required"));
        }

        try {
            User user = userService.register(
                    email, password, fullName, organizationName, currencySymbol, currencyCode, null, null, null
            );
            ApiToken token = tokenService.createToken(user);
            Organization org = orgService.findById(user.getOrganizationId()).orElse(null);

            auditLogService.record(
                    user.getOrganizationId(),
                    user.getId(),
                    deviceIdentityService.localDeviceId(),
                    "SIGNUP",
                    "User",
                    user.getId().toString(),
                    "New organization registered: " + organizationName
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(token.getToken(), userService.toDTO(user), orgService.toDTO(org)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findById(principal.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        Organization org = orgService.findById(principal.getOrganizationId()).orElse(null);
        return ResponseEntity.ok(Map.of(
                "user", userService.toDTO(user),
                "organization", orgService.toDTO(org)
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke current bearer token")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenService.revokeToken(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
