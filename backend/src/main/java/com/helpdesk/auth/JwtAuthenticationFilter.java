package com.helpdesk.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final RequestAttributeSecurityContextRepository securityContextRepository =
            new RequestAttributeSecurityContextRepository();

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

            log.debug(
                    "JWT authentication filter processing {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            userRepository.findById(userId).ifPresent(user -> {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                java.util.Collections.emptyList()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                securityContextRepository.saveContext(
                        SecurityContextHolder.getContext(),
                        request,
                        response
                );
            });
        }

        filterChain.doFilter(request, response);
    }
}
