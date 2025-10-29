package com.concurrent_web_crawler.auth.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secret;
    private long accessTtlSeconds = 1800; // 30min
    private long refreshTtlSeconds = 1209600; // 14d

}
