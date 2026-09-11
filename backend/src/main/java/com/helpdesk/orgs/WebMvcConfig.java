package com.helpdesk.orgs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OrganizationContextInterceptor organizationContextInterceptor;

    public WebMvcConfig(
            OrganizationContextInterceptor organizationContextInterceptor) {
        this.organizationContextInterceptor =
                organizationContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(organizationContextInterceptor)
                .addPathPatterns("/api/orgs/{organizationId}/**");
    }
}
