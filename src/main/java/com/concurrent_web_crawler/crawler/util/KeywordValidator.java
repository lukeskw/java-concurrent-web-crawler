package com.concurrent_web_crawler.crawler.util;

public final class KeywordValidator {
    private KeywordValidator() {}

    public static void validateKeyword(String k) {
        if (k == null) throw new IllegalArgumentException("Keyword is required");
        int len = k.trim().length();
        if (len < 4 || len > 32) throw new IllegalArgumentException("Keyword must have between 4 and 32 characters");
    }
}
