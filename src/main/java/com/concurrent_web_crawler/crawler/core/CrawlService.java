package com.concurrent_web_crawler.crawler.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlerEngine crawlerEngine;

    private final Map<String, CrawlState> states = new ConcurrentHashMap<>();

    public String start(String keyword) {
        validateKeyword(keyword);
        String id = generateId();
        var normalized = keyword.trim();
        var state = new CrawlState(id, normalized);
        states.put(id, state);
        crawlerEngine.crawlAsync(state);
        return id;
    }

    public CrawlState getState(String id) {
        var s = states.get(id);
        if (s == null) throw new NoSuchElementException("id não encontrado");
        return s;
    }

    private static void validateKeyword(String k) {
        if (k == null) throw new IllegalArgumentException("keyword obrigatória");
        int len = k.trim().length();
        if (len < 4 || len > 32) throw new IllegalArgumentException("keyword deve ter entre 4 e 32 caracteres");
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

        CrawlState(String id, String keyword) {
            this.id = id;
            this.keyword = keyword;
        }

        public boolean done() { return done; }
        public List<String> results() { return results.stream().sorted().toList(); }
        void markDone() { this.done = true; }

        public String id() { return id; }
        public String keyword() { return keyword; }
        public Set<String> visited() { return visited; }
        public ConcurrentLinkedQueue<String> frontier() { return frontier; }

        public void addResult(String url) {
            results.add(url);
        }
    }
}
