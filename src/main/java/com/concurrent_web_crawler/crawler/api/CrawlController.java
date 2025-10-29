package com.concurrent_web_crawler.crawler.api;

import com.concurrent_web_crawler.crawler.core.CrawlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlController {

    private final CrawlService crawlService;

    @PostMapping(path = "/crawl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> start(@Valid @RequestBody StartRequest body) {
        String id = crawlService.start(body.getKeyword());
        return Map.of("id", id);
    }

    @GetMapping("/crawl/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        try {
            var state = crawlService.getState(id);
            return ResponseEntity.ok(new CrawlResponse(
                    state.id(),
                    state.done() ? "done" : "active",
                    state.results()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "crawl_not_found",
                            "message", e.getMessage() != null ? e.getMessage() : "Crawl id não encontrado",
                            "id", id
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "crawl_error",
                            "message", e.getMessage() != null ? e.getMessage() : "Erro ao obter estado do crawl",
                            "id", id
                    ));
        }
    }

    @Data
    public static class StartRequest {
        @NotBlank
        @Size(min = 4, max = 32)
        private String keyword;
    }

    public record CrawlResponse(String id, String status, java.util.List<String> urls) {}
}
