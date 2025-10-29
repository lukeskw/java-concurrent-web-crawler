package com.concurrent_web_crawler.crawler.infra.executor;

import com.concurrent_web_crawler.crawler.model.CrawlState;
import com.concurrent_web_crawler.crawler.port.out.CrawlStarterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a\\s+[^>]*?href\\s*=\\s*['\"][^'\"]+['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

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
        if (containsKeyword(body, state.getKeyword())) {
            state.addResult(urlStr);
        }
        for (String link : extractLinks(body)) {
            String normalized = normalizeAndFilterUrl(link);
            if (normalized == null) continue;
            if (state.getVisited().contains(normalized)) continue;
            if (state.getFrontier().size() >= MAX_FRONTIER) continue;
            if (!underLimits(state)) continue;
            state.getFrontier().add(normalized);
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
                        String v = href.substring(open + 1, close).trim();
                        if (!v.isEmpty() && !v.startsWith("#") && !v.startsWith("javascript:") && !v.startsWith("mailto:")) {
                            links.add(v);
                        }
                    }
                }
            }
        }
        return links;
    }

    private String normalizeAndFilterUrl(String href) {
        try {
            URI base = URI.create(baseUrl);
            URI resolved = base.resolve(href);
            if (!sameBase(base, resolved)) return null;
            URI normalized = resolved.normalize();
            normalized = new URI(
                    normalized.getScheme(),
                    normalized.getUserInfo(),
                    normalized.getHost(),
                    normalized.getPort(),
                    ensureLeadingSlash(normalized.getPath()),
                    normalized.getQuery(),
                    null
            );
            String s = normalized.toString();
            if (!s.startsWith(base.toString())) return null;
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean sameBase(URI base, URI uri) {
        if (uri.getScheme() == null && uri.getHost() == null) return true;
        if (uri.getScheme() != null && !base.getScheme().equalsIgnoreCase(uri.getScheme())) return false;
        if (!safeEquals(base.getHost(), uri.getHost())) return false;
        int basePort = base.getPort() == -1 ? defaultPort(base) : base.getPort();
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
