package com.concurrent_web_crawler.auth.dto;

import java.util.List;

public record MeResponse(String username, List<String> roles) {}
