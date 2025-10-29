package com.concurrent_web_crawler.crawler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartCrawlRequest(@NotBlank @Size(min = 4, max = 32) String keyword) {}