package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.DashboardSummaryDTO;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportWebController {

    private final ReportService reportService;

    public ReportWebController(ReportService reportService) {
        this.reportService = reportService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        DashboardSummaryDTO summary = reportService.getDashboardSummary(orgId);
        model.addAttribute("summary", summary);
        return "reports/index";
    }
}
