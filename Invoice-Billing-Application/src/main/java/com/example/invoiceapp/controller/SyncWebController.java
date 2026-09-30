package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.SyncStatusDTO;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.service.AppRuntimeService;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.SyncQueueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sync")
public class SyncWebController {

    private final AppRuntimeService appRuntimeService;
    private final SyncQueueService syncQueueService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public SyncWebController(AppRuntimeService appRuntimeService,
                             SyncQueueService syncQueueService,
                             AuditLogService auditLogService,
                             DeviceIdentityService deviceIdentityService) {
        this.appRuntimeService = appRuntimeService;
        this.syncQueueService = syncQueueService;
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
    public String index(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        SyncStatusDTO status = appRuntimeService.getSyncStatus();
        model.addAttribute("syncStatus", status);
        model.addAttribute("queueItems", syncQueueService.listQueue(orgId));
        return "sync/index";
    }

    @PostMapping("/mode")
    public String switchMode(@RequestParam String targetMode,
                             @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            DataMode mode = DataMode.valueOf(targetMode.toUpperCase());
            if (mode == DataMode.CLOUD_SYNC) {
                if (!confirm) {
                    redirectAttributes.addFlashAttribute("error", "You must explicitly confirm before switching to Cloud Sync mode.");
                    return "redirect:/sync";
                }
                appRuntimeService.switchToCloudSync(true);
                auditLogService.record(
                        user.getOrganizationId(),
                        user.getId(),
                        deviceIdentityService.localDeviceId(),
                        "SWITCH_MODE_CLOUD_SYNC",
                        "AppRuntime",
                        "1",
                        "Switched to Cloud Sync mode with user confirmation"
                );
                redirectAttributes.addFlashAttribute("message", "Cloud Sync mode enabled! Device will queue and synchronize changes.");
            } else {
                appRuntimeService.switchToLocalOnly();
                auditLogService.record(
                        user.getOrganizationId(),
                        user.getId(),
                        deviceIdentityService.localDeviceId(),
                        "SWITCH_MODE_LOCAL_ONLY",
                        "AppRuntime",
                        "1",
                        "Switched to Local Only mode"
                );
                redirectAttributes.addFlashAttribute("message", "Local Only mode active. All data remains strictly on this device.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to switch mode: " + e.getMessage());
        }

        return "redirect:/sync";
    }

    @PostMapping("/trigger")
    public String triggerSync(RedirectAttributes redirectAttributes) {
        if (appRuntimeService.isLocalOnly()) {
            redirectAttributes.addFlashAttribute("error", "Cannot synchronize while in LOCAL_ONLY mode.");
            return "redirect:/sync";
        }

        int count = syncQueueService.processPendingQueue();
        redirectAttributes.addFlashAttribute("message", "Processed " + count + " queued synchronization items.");
        return "redirect:/sync";
    }
}
