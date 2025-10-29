package com.concurrent_web_crawler.auth.dto;

import com.concurrent_web_crawler.auth.port.out.LoginResponseUnion;

public record LoginErrorResponse(String error, String message) implements LoginResponseUnion {}
