package com.concurrent_web_crawler.crawler.web;

import com.concurrent_web_crawler.crawler.dto.CrawlResponse;
import com.concurrent_web_crawler.crawler.dto.StartCrawlRequest;
import com.concurrent_web_crawler.crawler.dto.StartResponse;
import com.concurrent_web_crawler.crawler.enumerator.CrawlStatus;
import com.concurrent_web_crawler.crawler.service.CrawlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlController {

    private final CrawlService crawlService;

    @PostMapping(path = "/crawl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartResponse> start(@Valid @RequestBody StartCrawlRequest body) {
        String id = crawlService.start(body.keyword());
        return ResponseEntity.accepted()
                .location(URI.create("/crawl/" + id))
                .body(new StartResponse(id));
    }

    @GetMapping("/crawl/{id}")
    public ResponseEntity<CrawlResponse> get(@PathVariable String id) {
        try {
            var state = crawlService.getState(id);
            return ResponseEntity.ok(new CrawlResponse(
                    state.id(),
                    state.done() ? CrawlStatus.DONE : CrawlStatus.ACTIVE,
                    state.results()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}