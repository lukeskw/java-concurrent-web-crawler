package com.concurrent_web_crawler.auth.dto;

import com.concurrent_web_crawler.auth.port.out.LogoutResponseUnion;

public record LogoutErrorResponse(String error) implements LogoutResponseUnion {}
