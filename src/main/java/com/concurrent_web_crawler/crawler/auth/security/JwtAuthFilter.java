package com.concurrent_web_crawler.crawler.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTokenBlacklist blacklist;

    public JwtAuthFilter(JwtService jwtService, RedisTokenBlacklist blacklist) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jws<Claims> jws = jwtService.parse(token);
                String jti = jws.getPayload().getId();
                if (jti != null && blacklist.isBlacklisted(jti)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                String subject = jws.getPayload().getSubject();
                List<String> roles = (List<String>) jws.getPayload().get("roles", List.class);
                Collection<GrantedAuthority> authorities = roles == null ? List.<GrantedAuthority>of()
                        : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                AbstractAuthenticationToken auth = new AbstractAuthenticationToken(authorities) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return subject; }
                };
                auth.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public static String newJti() {
        return UUID.randomUUID().toString();
    }

    public static Duration remainingTtl(Jws<Claims> jws) {
        long now = System.currentTimeMillis();
        long exp = jws.getPayload().getExpiration().getTime();
        long ms = Math.max(0, exp - now);
        return Duration.ofMillis(ms);
    }
}
