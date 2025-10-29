package com.concurrent_web_crawler.crawler.core;

import com.concurrent_web_crawler.crawler.infra.CrawlJob;
import com.concurrent_web_crawler.crawler.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrawlerEngine {

    private final CrawlJob crawlJob;

    public void crawlAsync(CrawlService.CrawlState state) {
        crawlJob.start(state);
    }
}