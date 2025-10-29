package com.concurrent_web_crawler.crawler.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final JwtProperties props;
    private Key key;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    private Key key() {
        if (key == null) {
            String secret = props.getSecret();
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("JWT secret não configurado (security.jwt.secret)");
            }
            if (secret.length() < 32) {
                throw new IllegalStateException("JWT secret deve ter pelo menos 32 caracteres");
            }
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }

    public String generateToken(String subject, Map<String, Object> claims, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claims(claims)
                .signWith(key())
                .compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) key()).build().parseSignedClaims(token);
    }
}
