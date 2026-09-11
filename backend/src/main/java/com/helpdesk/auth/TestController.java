package com.helpdesk.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public Map<String, Object> protectedEndpoint(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return Map.of(
                "message", "You are authenticated",
                "userId", user.getId(),
                "email", user.getEmail()
        );
    }
}
