package com.example.invoiceapp.security;

import com.example.invoiceapp.model.ApiToken;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.repository.UserRepository;
import com.example.invoiceapp.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiTokenFilter extends OncePerRequestFilter {

    private final ApiTokenService apiTokenService;
    private final UserRepository userRepository;

    public ApiTokenFilter(ApiTokenService apiTokenService, UserRepository userRepository) {
        this.apiTokenService = apiTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            Optional<ApiToken> tokenOpt = apiTokenService.validateToken(token);
            if (tokenOpt.isPresent()) {
                ApiToken apiToken = tokenOpt.get();
                userRepository.findById(apiToken.getUserId()).ifPresent(user -> {
                    if (user.isActive()) {
                        AppUserPrincipal principal = new AppUserPrincipal(user);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        } else {
            // Support existing browser session authentication
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = (User) session.getAttribute("user");
                if (user.isActive()) {
                    AppUserPrincipal principal = new AppUserPrincipal(user);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
