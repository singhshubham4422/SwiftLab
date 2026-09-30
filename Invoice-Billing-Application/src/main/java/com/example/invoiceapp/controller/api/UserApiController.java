package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.UserDTO;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.UserRole;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Organization user and role management APIs")
public class UserApiController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public UserApiController(UserService userService,
                             AuditLogService auditLogService,
                             DeviceIdentityService deviceIdentityService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all users in the organization")
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UserDTO> users = userService.listByOrganization(principal.getOrganizationId());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Operation(summary = "Add a new user to the organization")
    public ResponseEntity<?> createUser(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AccessPolicy.canManageUsers(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only OWNER and ADMIN can add organization users."));
        }

        String email = body.get("email");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String roleStr = body.getOrDefault("role", "STAFF");
        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            role = UserRole.STAFF;
        }

        try {
            User created = userService.createOrgUser(principal.getOrganizationId(), email, password, fullName, role);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "CREATE_USER",
                    "User",
                    created.getId().toString(),
                    "Created user: " + email + " with role: " + role
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.toDTO(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<?> updateRole(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AccessPolicy.canManageUsers(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only OWNER and ADMIN can change user roles."));
        }

        String roleStr = body.get("role");
        if (roleStr == null) return ResponseEntity.badRequest().body(Map.of("error", "role is required"));

        UserRole role = UserRole.valueOf(roleStr.toUpperCase());
        User updated = userService.updateRole(id, principal.getOrganizationId(), role);

        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "UPDATE_USER_ROLE",
                "User",
                updated.getId().toString(),
                "Changed role to: " + role
        );

        return ResponseEntity.ok(userService.toDTO(updated));
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<?> toggleActive(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AccessPolicy.canManageUsers(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only OWNER and ADMIN can enable/disable users."));
        }

        User updated = userService.toggleActive(id, principal.getOrganizationId());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "TOGGLE_USER_ACTIVE",
                "User",
                updated.getId().toString(),
                "Active status is now: " + updated.isActive()
        );

        return ResponseEntity.ok(userService.toDTO(updated));
    }
}
