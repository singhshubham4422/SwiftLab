package com.example.invoiceapp.controller;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.CompanySettingsService;
import com.example.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final CompanySettingsService settingsService;

    public AuthController(UserService userService, CompanySettingsService settingsService) {
        this.userService = userService;
        this.settingsService = settingsService;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (session != null && session.getAttribute("user") != null) {
            return "redirect:/invoices";
        }
        if (userService.countUsers() == 0) {
            return "redirect:/signup";
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        CompanySettings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        Optional<User> userOpt = userService.authenticate(email, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            return "redirect:/invoices";
        }

        CompanySettings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        model.addAttribute("error", "Invalid email or password. Please try again.");
        model.addAttribute("enteredEmail", email);
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(HttpSession session, Model model) {
        if (session != null && session.getAttribute("user") != null) {
            return "redirect:/invoices";
        }
        CompanySettings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        model.addAttribute("hasExistingUsers", userService.countUsers() > 0);
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@RequestParam("fullName") String fullName,
                                @RequestParam("organizationName") String organizationName,
                                @RequestParam("email") String email,
                                @RequestParam("password") String password,
                                @RequestParam(value = "currencySymbol", defaultValue = "₹") String currencySymbol,
                                @RequestParam(value = "currencyCode", defaultValue = "INR") String currencyCode,
                                @RequestParam(value = "tagline", required = false) String tagline,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "address", required = false) String address,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            User user = userService.register(
                    email,
                    password,
                    fullName,
                    organizationName,
                    currencySymbol,
                    currencyCode,
                    tagline,
                    phone,
                    address
            );

            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());

            redirectAttributes.addFlashAttribute("message",
                    "Welcome, " + user.getFullName() + "! Your organization " + user.getOrganizationName() + " Invoicing is ready.");
            return "redirect:/invoices";
        } catch (IllegalArgumentException e) {
            CompanySettings settings = settingsService.getSettings();
            model.addAttribute("settings", settings);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("organizationName", organizationName);
            model.addAttribute("email", email);
            model.addAttribute("hasExistingUsers", userService.countUsers() > 0);
            return "signup";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}
