package com.helpdesk.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long websocketTicketExpirationMs;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.websocket-ticket-expiration-ms}") long websocketTicketExpirationMs,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMs = expirationMs;
        this.websocketTicketExpirationMs = websocketTicketExpirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(User user) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiration)
                .issuer(issuer)
                .audience().add(audience).and()
                .signWith(secretKey)
                .compact();
    }

    public String generateWebSocketTicket(User user) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + websocketTicketExpirationMs
        );

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "websocket")
                .issuedAt(now)
                .expiration(expiration)
                .issuer(issuer)
                .audience().add(audience).and()
                .signWith(secretKey)
                .compact();
    }

    public boolean isWebSocketTicketValid(String token) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "websocket".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public boolean isTokenValid(String token) {

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
