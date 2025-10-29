package com.concurrent_web_crawler.crawler.service;

import com.concurrent_web_crawler.crawler.dto.CrawlStateDto;
import com.concurrent_web_crawler.crawler.model.CrawlState;
import com.concurrent_web_crawler.crawler.port.out.CrawlStarterPort;
import com.concurrent_web_crawler.crawler.util.IdUtils;
import com.concurrent_web_crawler.crawler.util.KeywordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlStarterPort crawlStarter;
    private final CacheManager cacheManager;

    private final Map<String, CrawlState> states = new ConcurrentHashMap<>();

    public String start(String keyword) {
        KeywordValidator.validateKeyword(keyword);
        String id = IdUtils.generateId();
        var normalized = keyword.trim();
        var state = new CrawlState(id, normalized, this::onStateDone);
        states.put(id, state);
        crawlStarter.start(state); // kicks off async work via out port
        return id;
    }

    @Cacheable(cacheNames = "crawlState", key = "#id")
    public CrawlStateDto getState(String id) {
        var s = states.get(id);
        if (s == null) throw new NoSuchElementException("ID not found");
        return CrawlStateDto.from(s);
    }

    private void onStateDone(String id, CrawlState finalState) {
        Cache inProgress = cacheManager.getCache("crawlState");
        if (inProgress != null) inProgress.evict(id);

        Cache finalCache = cacheManager.getCache("crawlStateFinal");
        if (finalCache != null) finalCache.put(id, CrawlStateDto.from(finalState));
    }
}
