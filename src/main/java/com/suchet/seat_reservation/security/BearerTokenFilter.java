package com.suchet.seat_reservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean requiresUserAuthentication =
                (
                        request.getMethod().equals("POST")
                                && path.matches("/shows/[^/]+/reserve")
                )
                        ||
                        (
                                request.getMethod().equals("POST")
                                        && path.matches("/reservations/[^/]+/cancel")
                        );

        if (!requiresUserAuthentication) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"missing_or_invalid_authorization\"}"
            );
            return;
        }

        String userId = authorization
                .substring(7)
                .trim();

        if (userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"missing_or_invalid_authorization\"}"
            );
            return;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId);

        filterChain.doFilter(request, response);
    }
}