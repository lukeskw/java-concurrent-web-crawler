package com.concurrent_web_crawler.crawler.util;

public final class KeywordUtils {
    private KeywordUtils() {}

    public static void validateKeyword(String k) {
        if (k == null) throw new IllegalArgumentException("Keyword is required");
        int len = k.trim().length();
        if (len < 4 || len > 64) throw new IllegalArgumentException("Keyword must have between 4 and 64 characters");
    }

    public static String normalize(String keyword) {
        if (keyword == null) return null;
        validateKeyword(keyword);
        return keyword.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
