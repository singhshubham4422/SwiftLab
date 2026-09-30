package com.example.invoiceapp.controller;

import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.DeviceIdentityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/devices")
public class DeviceWebController {

    private final DeviceIdentityService deviceIdentityService;

    public DeviceWebController(DeviceIdentityService deviceIdentityService) {
        this.deviceIdentityService = deviceIdentityService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("devices", deviceIdentityService.list(orgId));
        model.addAttribute("currentDeviceId", deviceIdentityService.localDeviceId());
        return "devices/list";
    }
}
