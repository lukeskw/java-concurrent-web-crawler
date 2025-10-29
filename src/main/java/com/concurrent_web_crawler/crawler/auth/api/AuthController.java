package com.concurrent_web_crawler.crawler.auth.api;

import com.concurrent_web_crawler.crawler.auth.security.JwtAuthFilter;
import com.concurrent_web_crawler.crawler.auth.security.JwtProperties;
import com.concurrent_web_crawler.crawler.auth.security.JwtService;
import com.concurrent_web_crawler.crawler.auth.security.RedisTokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties props;
    private final RedisTokenBlacklist blacklist;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          JwtProperties props,
                          RedisTokenBlacklist blacklist) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.props = props;
        this.blacklist = blacklist;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));
            UserDetails principal = (UserDetails) auth.getPrincipal();
            String jti = JwtAuthFilter.newJti();
            String access = jwtService.generateToken(
                    principal.getUsername(),
                    Map.of(
                            Claims.ID, jti,
                            "roles", principal.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                    ),
                    props.getAccessTtlSeconds()
            );
            String refresh = jwtService.generateToken(
                    principal.getUsername(),
                    Map.of(
                            Claims.ID, "r-" + jti
                    ),
                    props.getRefreshTtlSeconds()
            );
            return ResponseEntity.ok(Map.of(
                    "token_type", "Bearer",
                    "access_token", access,
                    "expires_in", props.getAccessTtlSeconds(),
                    "refresh_token", refresh
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "bad_credentials",
                    "message", "Usuário ou senha inválidos"
            ));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "user_not_found",
                    "message", "Usuário não encontrado"
            ));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "user_disabled",
                    "message", "Usuário desabilitado"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "auth_error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authz) {
        if (authz == null || !authz.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing bearer"));
        }
        String token = authz.substring(7);
        try {
            Jws<io.jsonwebtoken.Claims> jws = jwtService.parse(token);
            String jti = jws.getPayload().getId();
            if (jti != null) {
                blacklist.blacklist(jti, JwtAuthFilter.remainingTtl(jws));
            }
            return ResponseEntity.ok(Map.of("status", "logged_out"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return ResponseEntity.ok(Map.of(
                "username", principal.getUsername(),
                "roles", roles
        ));
    }

    public record LoginRequest(@NotBlank String usernameOrEmail, @NotBlank String password) {}
}
