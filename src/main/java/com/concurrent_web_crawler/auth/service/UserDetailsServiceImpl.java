package com.concurrent_web_crawler.auth.service;

import com.concurrent_web_crawler.auth.model.UserAccount;
import com.concurrent_web_crawler.auth.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository repo;

    public UserDetailsServiceImpl(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        UserAccount ua = repo.findByUsername(usernameOrEmail)
                .or(() -> repo.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(ua.getUsername())
                .password(ua.getPasswordHash())
                .authorities(ua.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(Collectors.toList()))
                .disabled(!ua.isEnabled())
                .build();
    }
}
