package com.concurrent_web_crawler.crawler.infra.executor;

import com.concurrent_web_crawler.crawler.model.CrawlState;
import com.concurrent_web_crawler.crawler.port.out.CrawlStarterPort;
import com.concurrent_web_crawler.crawler.util.HtmlUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

@Component
@RequiredArgsConstructor
public class CrawlJob implements CrawlStarterPort {

    private static final int MAX_RESULTS = 100;
    private static final int MAX_PAGES   = 10_000;
    private static final int MAX_FRONTIER = 50_000;

    private final RestTemplate http;
    private final ExecutorService virtualThreadExecutor;

    @Value("${crawler.base-url}")
    private String baseUrl;

    @Override
    public void start(CrawlState state) {
        virtualThreadExecutor.submit(() -> {
            state.getFrontier().add(baseUrl);
            runWaves(state);
        });
    }

    private void runWaves(CrawlState state) {
        while (!state.getFrontier().isEmpty() && underLimits(state)) {
            submitWave(state);
        }
        state.markDone(); // mark once, idempotent
    }

    private void submitWave(CrawlState state) {
        var phaser = new Phaser(1);
        while (!state.getFrontier().isEmpty() && underLimits(state)) {
            String url = state.getFrontier().poll();
            if (url == null) break;
            if (!state.getVisited().add(url)) continue;

            phaser.register();
            virtualThreadExecutor.submit(() -> {
                try {
                    processUrl(state, url);
                } catch (Exception ignored) {
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
        phaser.arriveAndDeregister();
        try {
            phaser.awaitAdvanceInterruptibly(0);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean underLimits(CrawlState s) {
        return s.resultsCount() < MAX_RESULTS && s.getVisited().size() < MAX_PAGES;
    }

    private void processUrl(CrawlState state, String urlStr) {
        var resp = http.getForEntity(urlStr, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return;
        var ct = resp.getHeaders().getContentType();
        if (ct == null || !"text".equalsIgnoreCase(ct.getType()) || !"html".equalsIgnoreCase(ct.getSubtype())) return;

        String body = resp.getBody();
        if (HtmlUrlUtils.containsKeyword(body, state.getKeyword())) {
            state.addResult(urlStr);
        }
        Set<String> links = HtmlUrlUtils.extractLinks(body);
        for (String link : links) {
            String normalized = HtmlUrlUtils.normalizeAndFilterUrl(baseUrl, link);
            if (normalized == null) continue;
            if (state.getVisited().contains(normalized)) continue;
            if (state.getFrontier().size() >= MAX_FRONTIER) continue;
            if (!underLimits(state)) continue;
            state.getFrontier().add(normalized);
        }
    }
}