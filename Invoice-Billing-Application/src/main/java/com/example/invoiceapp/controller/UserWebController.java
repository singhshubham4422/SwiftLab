package com.example.invoiceapp.controller;

import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.UserRole;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserWebController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public UserWebController(UserService userService,
                             AuditLogService auditLogService,
                             DeviceIdentityService deviceIdentityService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    private User getSessionUser(HttpSession session) {
        return session != null ? (User) session.getAttribute("user") : null;
    }

    private Long getOrgId(HttpSession session) {
        User u = getSessionUser(session);
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("users", userService.listByOrganization(orgId));
        model.addAttribute("roles", UserRole.values());
        return "users/list";
    }

    @PostMapping("/save")
    public String save(@RequestParam String email,
                       @RequestParam String password,
                       @RequestParam String fullName,
                       @RequestParam String role,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        User current = getSessionUser(session);
        if (current == null || current.getOrganizationId() == null) return "redirect:/login";

        if (!AccessPolicy.canManageUsers(current.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Only OWNER and ADMIN can add new users.");
            return "redirect:/users";
        }

        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            User created = userService.createOrgUser(current.getOrganizationId(), email, password, fullName, userRole);
            auditLogService.record(
                    current.getOrganizationId(),
                    current.getId(),
                    deviceIdentityService.localDeviceId(),
                    "CREATE_USER",
                    "User",
                    created.getId().toString(),
                    "Added user " + email + " (" + userRole + ")"
            );
            redirectAttributes.addFlashAttribute("message", "User '" + fullName + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add user: " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam String role,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User current = getSessionUser(session);
        if (current == null || current.getOrganizationId() == null) return "redirect:/login";

        if (!AccessPolicy.canManageUsers(current.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Only OWNER and ADMIN can update roles.");
            return "redirect:/users";
        }

        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            userService.updateRole(id, current.getOrganizationId(), userRole);
            redirectAttributes.addFlashAttribute("message", "User role updated to " + userRole);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update role: " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User current = getSessionUser(session);
        if (current == null || current.getOrganizationId() == null) return "redirect:/login";

        if (!AccessPolicy.canManageUsers(current.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Only OWNER and ADMIN can toggle user status.");
            return "redirect:/users";
        }

        try {
            User updated = userService.toggleActive(id, current.getOrganizationId());
            redirectAttributes.addFlashAttribute("message", "User status changed to " + (updated.isActive() ? "Active" : "Disabled"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }

        return "redirect:/users";
    }
}
