package com.concurrent_web_crawler.crawler.service;

import com.concurrent_web_crawler.crawler.model.CrawlState;
import com.concurrent_web_crawler.crawler.port.out.CrawlStarterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlStarterPort crawlStarter;
    private final CacheManager cacheManager;

    private final Map<String, CrawlState> states = new ConcurrentHashMap<>();

    public String start(String keyword) {
        validateKeyword(keyword);
        String id = generateId();
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

    private static void validateKeyword(String k) {
        if (k == null) throw new IllegalArgumentException("Keyword is required");
        int len = k.trim().length();
        if (len < 4 || len > 32) throw new IllegalArgumentException("Keyword must have between 4 and 32 characters");
    }

    private static String generateId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public record CrawlStateDto(
            String id,
            String keyword,
            List<String> results,
            List<String> visited,
            List<String> frontier,
            boolean done
    ) implements Serializable {
        public static CrawlStateDto from(CrawlState s) {
            List<String> results = new ArrayList<>(s.results());
            List<String> visited = new ArrayList<>(s.getVisited());
            List<String> frontier = new ArrayList<>(s.getFrontier());
            return new CrawlStateDto(s.getId(), s.getKeyword(), results, visited, frontier, s.done());
        }
    }
}
