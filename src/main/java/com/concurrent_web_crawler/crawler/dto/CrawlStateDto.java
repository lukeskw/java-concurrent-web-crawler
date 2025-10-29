package com.concurrent_web_crawler.crawler.dto;

import com.concurrent_web_crawler.crawler.model.CrawlState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
