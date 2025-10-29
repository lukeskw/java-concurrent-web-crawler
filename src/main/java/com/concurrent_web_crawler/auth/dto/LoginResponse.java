package com.concurrent_web_crawler.auth.dto;

import com.concurrent_web_crawler.auth.port.out.LoginResponseUnion;

public record LoginResponse(String token_type, String access_token, long expires_in, String refresh_token)
        implements LoginResponseUnion {}
