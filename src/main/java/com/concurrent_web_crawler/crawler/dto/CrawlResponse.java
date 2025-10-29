package com.concurrent_web_crawler.crawler.dto;

import com.concurrent_web_crawler.crawler.enumerator.CrawlStatus;

import java.util.List;

public record CrawlResponse(String id, CrawlStatus status, List<String> urls) {}