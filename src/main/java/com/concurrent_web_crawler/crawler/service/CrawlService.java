package com.concurrent_web_crawler.crawler.service;

import com.concurrent_web_crawler.crawler.core.CrawlerEngine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlerEngine crawlerEngine;
    private final CacheManager cacheManager;

    private final Map<String, CrawlState> states = new ConcurrentHashMap<>();

    public String start(String keyword) {
        validateKeyword(keyword);
        String id = generateId();
        var normalized = keyword.trim();
        var state = new CrawlState(id, normalized, this::onStateDone);
        states.put(id, state);
        crawlerEngine.crawlAsync(state);
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
        if (inProgress != null) {
            inProgress.evict(id);
        }
        Cache finalCache = cacheManager.getCache("crawlStateFinal");
        if (finalCache != null) {
            finalCache.put(id, CrawlStateDto.from(finalState));
        }
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

    @Getter
    @Setter
    public static final class CrawlState {
        private final String id;
        private final String keyword;
        private final Set<String> results = ConcurrentHashMap.newKeySet();
        private final Set<String> visited = ConcurrentHashMap.newKeySet();
        private final ConcurrentLinkedQueue<String> frontier = new ConcurrentLinkedQueue<>();
        private volatile boolean done = false;

        private final DoneCallback doneCallback;

        CrawlState(String id, String keyword, DoneCallback doneCallback) {
            this.id = id;
            this.keyword = keyword;
            this.doneCallback = doneCallback;
        }

        public boolean done() { return done; }
        public List<String> results() { return results.stream().sorted().toList(); }
        public void markDone() {
            this.done = true;
            if (doneCallback != null) doneCallback.onDone(id, this);
        }

        public String id() { return id; }
        public String keyword() { return keyword; }
        public Set<String> visited() { return visited; }
        public ConcurrentLinkedQueue<String> frontier() { return frontier; }

        public void addResult(String url) {
            results.add(url);
        }

        @FunctionalInterface
        public interface DoneCallback {
            void onDone(String id, CrawlState state);
        }
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
            List<String> visited = new ArrayList<>(s.visited());
            List<String> frontier = new ArrayList<>(s.frontier());
            return new CrawlStateDto(s.id(), s.keyword(), results, visited, frontier, s.done());
        }
    }
}
