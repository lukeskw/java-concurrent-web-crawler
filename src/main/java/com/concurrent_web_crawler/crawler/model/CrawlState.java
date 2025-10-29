package com.concurrent_web_crawler.crawler.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public final class CrawlState implements Serializable {
    private final String id;
    private final String keyword;

    private final Set<String> results = ConcurrentHashMap.newKeySet();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> frontier = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean done = new AtomicBoolean(false);
    private final DoneCallback doneCallback;

    public CrawlState(String id, String keyword, DoneCallback doneCallback) {
        this.id = id;
        this.keyword = keyword;
        this.doneCallback = doneCallback;
    }

    public boolean done() { return done.get(); }
    public int resultsCount() { return results.size(); }
    public List<String> results() { return results.stream().sorted().toList(); }

    public void addResult(String url) { results.add(url); }

    public void markDone() {
        if (done.compareAndSet(false, true) && doneCallback != null) {
            doneCallback.onDone(id, this);
        }
    }

    @FunctionalInterface
    public interface DoneCallback {
        void onDone(String id, CrawlState state);
    }
}
