package com.concurrent_web_crawler.crawler.service;

import com.concurrent_web_crawler.crawler.dto.CrawlStateDto;
import com.concurrent_web_crawler.crawler.model.CrawlState;
import com.concurrent_web_crawler.crawler.port.out.CrawlStarterPort;
import com.concurrent_web_crawler.crawler.util.IdUtils;
import com.concurrent_web_crawler.crawler.util.KeywordUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlStarterPort crawlStarter;
    private final CacheManager cacheManager;
    private final CrawlCacheService crawlCacheService;
    private final ObjectMapper objectMapper;

    private final Map<String, CrawlState> states = new ConcurrentHashMap<>();

    public String start(String keyword) {
        KeywordUtils.validateKeyword(keyword);

        var crawlReq = crawlCacheService.upsertPending(keyword);

        String id = IdUtils.generateId();
        var normalized = KeywordUtils.normalize(keyword);
        var state = new CrawlState(id, normalized, this::onStateDone);
        states.put(id, state);

        crawlCacheService.markRunning(crawlReq.getId());
        crawlStarter.start(state);

        return id;
    }

    public CrawlStateDto getState(String id) {
        var s = states.get(id);
        if (s == null) throw new NoSuchElementException("ID not found");
        return CrawlStateDto.from(s);
    }

    private void onStateDone(String id, CrawlState finalState) {
        try {
            JsonNode resultJson = objectMapper.valueToTree(finalState.results());
            boolean success = !finalState.results().isEmpty();

            var req = crawlCacheService.getByKeyword(finalState.getKeyword());
            if (req != null) {
                crawlCacheService.saveResult(req.getId(), resultJson, success);
            }
        } catch (Exception ignored) {
            var req = crawlCacheService.getByKeyword(finalState.getKeyword());
            if (req != null) {
                crawlCacheService.saveResult(req.getId(), null, false);
            }
        }

        Cache inProgress = cacheManager.getCache("crawlState");
        if (inProgress != null) inProgress.evict(id);

        Cache finalCache = cacheManager.getCache("crawlStateFinal");
        if (finalCache != null) finalCache.put(id, CrawlStateDto.from(finalState));
    }
}
