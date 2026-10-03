package com.suchet.seat_reservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    @Value("${app.admin-token}")
    private String adminToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        boolean isCreateShowRequest =
                request.getMethod().equals("POST")
                        && request.getRequestURI().equals("/shows");

        if (!isCreateShowRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.equals("Bearer " + adminToken)) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"admin_authorization_required\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}