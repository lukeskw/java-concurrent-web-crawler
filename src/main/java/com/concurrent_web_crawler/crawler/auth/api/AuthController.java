package com.concurrent_web_crawler.crawler.auth.api;

import com.concurrent_web_crawler.crawler.auth.security.JwtAuthFilter;
import com.concurrent_web_crawler.crawler.auth.security.JwtProperties;
import com.concurrent_web_crawler.crawler.auth.security.JwtService;
import com.concurrent_web_crawler.crawler.auth.security.RedisTokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginResponseUnion> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));
            Object principalObj = auth.getPrincipal();
            String username = (principalObj instanceof UserDetails ud) ? ud.getUsername() : auth.getName();
            List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            String jti = JwtAuthFilter.newJti();
            String access = jwtService.generateToken(
                    username,
                    Map.of(
                            Claims.ID, jti,
                            "roles", roles
                    ),
                    props.getAccessTtlSeconds()
            );
            String refresh = jwtService.generateToken(
                    username,
                    Map.of(
                            Claims.ID, "r-" + jti
                    ),
                    props.getRefreshTtlSeconds()
            );
            return ResponseEntity.ok(new LoginResponse("Bearer", access, props.getAccessTtlSeconds(), refresh));
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(new LoginErrorResponse("invalid_credentials", "Invalid username or password"));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(new LoginErrorResponse("user_disabled", "User disabled"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new LoginErrorResponse("auth_error", "Authentication error"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<? extends LogoutResponseUnion> logout(
            @RequestHeader(name = "Authorization", required = false) String authz,
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenHeader) {
        if (authz == null || !authz.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new LogoutErrorResponse("missing_bearer"));
        }
        String accessToken = authz.substring(7);
        try {
            Jws<io.jsonwebtoken.Claims> accessJws = jwtService.parse(accessToken);
            String accessJti = accessJws.getPayload().getId();
            if (accessJti != null) {
                blacklist.blacklist(accessJti, JwtAuthFilter.remainingTtl(accessJws));
            }
            if (refreshTokenHeader != null && !refreshTokenHeader.isBlank()) {
                String refreshToken = refreshTokenHeader.startsWith("Bearer ") ? refreshTokenHeader.substring(7) : refreshTokenHeader;
                try {
                    Jws<io.jsonwebtoken.Claims> refreshJws = jwtService.parse(refreshToken);
                    String refreshJti = refreshJws.getPayload().getId();
                    if (refreshJti != null && refreshJti.startsWith("r-")) {
                        blacklist.blacklist(refreshJti, JwtAuthFilter.remainingTtl(refreshJws));
                    }
                } catch (Exception ignored) { }
            }
            return ResponseEntity.ok(new LogoutOkResponse("logged_out"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new LogoutErrorResponse("invalid_token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(new MeResponse(username, roles));
    }

    public record LoginRequest(@NotBlank String usernameOrEmail, @NotBlank String password) {}

    public record LoginResponse(String token_type, String access_token, long expires_in, String refresh_token) implements LoginResponseUnion {}

    public record LoginErrorResponse(String error, String message) implements LoginResponseUnion {}

    public interface LoginResponseUnion {}

    public record LogoutOkResponse(String status) implements LogoutResponseUnion {}

    public record LogoutErrorResponse(String error) implements LogoutResponseUnion {}

    public interface LogoutResponseUnion {}

    public record MeResponse(String username, List<String> roles) {}
}
