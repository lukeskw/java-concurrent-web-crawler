package com.concurrent_web_crawler.crawler.core;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CrawlJob {

    private static final int MAX_RESULTS = 100;
    private static final int MAX_PAGES = 10_000;

    private final RestTemplate http;
    private final ExecutorService virtualThreadExecutor;
    private final URI baseUri;

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a\\s+[^>]*?href\\s*=\\s*['\"][^'\"]+['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

    public void start(CrawlService.CrawlState state) {
        // dispara a mesma orquestração antiga em background
        virtualThreadExecutor.submit(() -> {
            state.frontier().add(baseUri.toString());
            submitWave(state);
        });
    }

    private void submitWave(CrawlService.CrawlState state) {
        var phaser = new Phaser(1); // registrar a controladora
        while (!state.frontier().isEmpty() && underLimits(state)) {
            String url = state.frontier().poll();
            if (url == null) break;
            if (!state.visited().add(url)) continue;

            phaser.register();
            virtualThreadExecutor.submit(() -> {
                try {
                    processUrl(state, url);
                } catch (Exception ignored) {
                    // falhas de rede/parse são ignoradas
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
        if (!state.frontier().isEmpty() && underLimits(state)) {
            submitWave(state);
            return;
        }
        state.markDone();
    }

    private boolean underLimits(CrawlService.CrawlState s) {
        return s.results().size() < MAX_RESULTS && s.visited().size() < MAX_PAGES;
    }

    private void processUrl(CrawlService.CrawlState state, String urlStr) {
        ResponseEntity<String> resp = http.getForEntity(urlStr, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return;

        String body = resp.getBody();
        if (containsKeyword(body, state.keyword())) {
            state.addResult(urlStr);
        }
        for (String link : extractLinks(body)) {
            String normalized = normalizeAndFilterUrl(link);
            if (normalized == null) continue;
            if (!state.visited().contains(normalized)) {
                state.frontier().add(normalized);
            }
        }
    }

    private static boolean containsKeyword(String html, String keyword) {
        return html.toLowerCase().contains(keyword.toLowerCase());
    }

    private Set<String> extractLinks(String html) {
        Set<String> links = new HashSet<>();
        Matcher m = LINK_PATTERN.matcher(html);
        while (m.find()) {
            String href = m.group(0);
            int start = href.toLowerCase().indexOf("href");
            if (start >= 0) {
                int q1 = href.indexOf('"', start);
                int q2 = href.indexOf('\'', start);
                int open = (q1 == -1 || (q2 != -1 && q2 < q1)) ? q2 : q1;
                if (open != -1) {
                    char quote = href.charAt(open);
                    int close = href.indexOf(quote, open + 1);
                    if (close > open + 1) {
                        links.add(href.substring(open + 1, close).trim());
                    }
                }
            }
        }
        return links;
    }

    private String normalizeAndFilterUrl(String href) {
        try {
            String decoded = URLDecoder.decode(href, StandardCharsets.UTF_8);
            URI uri = baseUri.resolve(decoded);
            if (!sameBase(uri)) return null;
            URI normalized = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    ensureLeadingSlash(uri.getPath()),
                    uri.getQuery(),
                    null
            );
            String s = normalized.toString();
            if (!s.startsWith(baseUri.toString())) return null;
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean sameBase(URI uri) {
        if (uri.getScheme() == null) return true; // relativo
        if (!baseUri.getScheme().equalsIgnoreCase(uri.getScheme())) return false;
        if (!safeEquals(baseUri.getHost(), uri.getHost())) return false;
        int basePort = baseUri.getPort() == -1 ? defaultPort(baseUri) : baseUri.getPort();
        int uPort = uri.getPort() == -1 ? defaultPort(uri) : uri.getPort();
        return basePort == uPort;
    }

    private static int defaultPort(URI u) {
        return "https".equalsIgnoreCase(u.getScheme()) ? 443 : 80;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    private static String ensureLeadingSlash(String p) {
        if (p == null || p.isBlank()) return "/";
        return p.startsWith("/") ? p : "/" + p;
    }
}
