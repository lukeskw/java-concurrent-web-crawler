package com.concurrent_web_crawler.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usernameOrEmail, @NotBlank String password) {}
