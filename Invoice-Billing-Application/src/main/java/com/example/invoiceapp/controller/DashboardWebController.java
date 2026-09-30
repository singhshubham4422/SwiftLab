package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.DashboardSummaryDTO;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.ReportService;
import com.example.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardWebController {

    private final ReportService reportService;
    private final UserService userService;

    public DashboardWebController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    private User getSessionUser(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        DashboardSummaryDTO summary = reportService.getDashboardSummary(user.getOrganizationId());
        model.addAttribute("summary", summary);
        return "dashboard";
    }
}
