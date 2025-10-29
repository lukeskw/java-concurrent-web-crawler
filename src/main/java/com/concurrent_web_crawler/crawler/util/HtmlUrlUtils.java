package com.concurrent_web_crawler.crawler.util;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlUrlUtils {
    private HtmlUrlUtils() {}

    public static final Pattern LINK_PATTERN = Pattern.compile(
            "<a\\s+[^>]*?href\\s*=\\s*['\"][^'\"]+['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean containsKeyword(String html, String keyword) {
        return html.toLowerCase().contains(keyword.toLowerCase());
    }

    public static Set<String> extractLinks(String html) {
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

    public static String normalizeAndFilterUrl(String baseUrl, String href) {
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

    public static boolean sameBase(URI base, URI uri) {
        if (uri.getScheme() == null && uri.getHost() == null) return true;
        if (uri.getScheme() != null && !base.getScheme().equalsIgnoreCase(uri.getScheme())) return false;
        if (!safeEquals(base.getHost(), uri.getHost())) return false;
        int basePort = base.getPort() == -1 ? defaultPort(base) : base.getPort();
        int uPort = uri.getPort() == -1 ? defaultPort(uri) : uri.getPort();
        return basePort == uPort;
    }

    public static int defaultPort(URI u) {
        return "https".equalsIgnoreCase(u.getScheme()) ? 443 : 80;
    }

    public static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    public static String ensureLeadingSlash(String p) {
        if (p == null || p.isBlank()) return "/";
        return p.startsWith("/") ? p : "/" + p;
    }
}
