package com.concurrent_web_crawler.crawler.port.out;

import com.concurrent_web_crawler.crawler.model.CrawlState;

public interface CrawlStarterPort {
    void start(CrawlState state);
}
