package com.example.invoiceapp.controller;

import com.example.invoiceapp.model.AppRuntime;
import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.AppRuntimeService;
import com.example.invoiceapp.service.CompanySettingsService;
import com.example.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final CompanySettingsService settingsService;
    private final AppRuntimeService appRuntimeService;

    public AuthInterceptor(UserService userService,
                           CompanySettingsService settingsService,
                           AppRuntimeService appRuntimeService) {
        this.userService = userService;
        this.settingsService = settingsService;
        this.appRuntimeService = appRuntimeService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Allow public static assets and auth endpoints
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") 
                || path.equals("/favicon.ico") || path.startsWith("/login") || path.startsWith("/signup")
                || path.startsWith("/api/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")) {
            return true;
        }

        // If no user accounts exist yet in DB, redirect to signup page
        if (userService.countUsers() == 0) {
            response.sendRedirect("/signup");
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }

        // Otherwise redirect to login
        response.sendRedirect("/login");
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                modelAndView.addObject("currentUser", user);
                CompanySettings settings = settingsService.getSettingsForUser(user.getId());
                modelAndView.addObject("settings", settings);
                AppRuntime runtime = appRuntimeService.getRuntime();
                modelAndView.addObject("appRuntime", runtime);
                modelAndView.addObject("syncStatus", appRuntimeService.getSyncStatus());
            }
        }
    }
}
