package com.concurrent_web_crawler.crawler.util;

import java.util.concurrent.ThreadLocalRandom;

public final class IdUtils {
    private IdUtils() {}

    public static String generateId() {
        var chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
