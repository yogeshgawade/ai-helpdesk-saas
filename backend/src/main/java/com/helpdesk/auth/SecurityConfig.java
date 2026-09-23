package com.helpdesk.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.web.CorrelationIdFilter;
import com.helpdesk.security.RateLimitFilter;
import com.helpdesk.security.RedisRateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter redisRateLimiter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            RedisRateLimiter redisRateLimiter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.redisRateLimiter = redisRateLimiter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService) {

        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
        configuration.setAllowedMethods(java.util.List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(java.util.List.of(
                "Authorization", "Content-Type", "Accept",
                CorrelationIdFilter.HEADER_NAME
        ));
        configuration.setExposedHeaders(java.util.List.of(
                CorrelationIdFilter.HEADER_NAME
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) -> {
                                    String requestId =
                                            (String) request.getAttribute(
                                                    CorrelationIdFilter.ATTRIBUTE_NAME
                                            );

                                    if (requestId == null || requestId.isBlank()) {
                                        requestId = request.getHeader(
                                                CorrelationIdFilter.HEADER_NAME
                                        );
                                    }

                                    if (requestId == null || requestId.isBlank()) {
                                        requestId = UUID.randomUUID().toString();
                                    }

                                    ProblemDetail problemDetail =
                                            ProblemDetail.forStatusAndDetail(
                                                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                                                    "Authentication is required"
                                            );

                                    problemDetail.setTitle("Unauthorized");
                                    problemDetail.setType(
                                            java.net.URI.create("https://api.helpdesk.local/errors/unauthorized")
                                    );
                                    problemDetail.setInstance(
                                            java.net.URI.create(request.getRequestURI())
                                    );
                                    problemDetail.setProperty(
                                            "requestId",
                                            requestId
                                    );

                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );
                                    response.setContentType(
                                            MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                    );

                                    objectMapper.writeValue(
                                            response.getWriter(),
                                            problemDetail
                                    );
                                }
                        )
                )
                .securityContext(securityContext ->
                        securityContext
                                .securityContextRepository(
                                        new RequestAttributeSecurityContextRepository()
                                )
                )
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/ws").permitAll()
                                .requestMatchers("/actuator/health/**").permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new CorrelationIdFilter(),
                        SecurityContextHolderFilter.class
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        new RateLimitFilter(
                                redisRateLimiter,
                                objectMapper
                        ),
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}
