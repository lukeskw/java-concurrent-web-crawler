package com.concurrent_web_crawler.crawler.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrawlerEngine {

    private final CrawlJob crawlJob;

    public void crawlAsync(CrawlService.CrawlState state) {
        // mantém a API rápida e usa a mesma lógica antiga dentro do job
        crawlJob.start(state);
    }
}