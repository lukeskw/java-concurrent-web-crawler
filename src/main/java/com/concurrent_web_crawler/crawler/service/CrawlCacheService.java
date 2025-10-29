package com.concurrent_web_crawler.crawler.service;

import com.concurrent_web_crawler.crawler.enumerator.CrawlStatus;
import com.concurrent_web_crawler.crawler.model.CrawlRequest;
import com.concurrent_web_crawler.crawler.repository.CrawlRequestRepository;
import com.concurrent_web_crawler.crawler.util.KeywordUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlCacheService {

    private final CrawlRequestRepository repository;

    @Transactional(readOnly = true)
    public CrawlRequest getByKeyword(String keyword) {
        String norm = KeywordUtils.normalize(keyword);
        return repository.findByKeywordNormalized(norm).orElse(null);
    }

    @Transactional
    public CrawlRequest upsertPending(String keyword) {
        String norm = KeywordUtils.normalize(keyword);
        return repository.findByKeywordNormalized(norm)
                .orElseGet(() -> repository.save(
                        CrawlRequest.builder()
                                .keywordNormalized(norm)
                                .status(CrawlStatus.PENDING)
                                .build()
                ));
    }

    @Transactional
    public void markRunning(Long id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(CrawlStatus.ACTIVE);
//            repository.save(entity);
        });
    }

    @Transactional
    public void saveResult(Long id, JsonNode resultJson, boolean success) {
        repository.findById(id).ifPresent(entity -> {
            entity.setResultJson(resultJson);
            entity.setStatus(success ? CrawlStatus.DONE : CrawlStatus.FAILED);
//            repository.save(entity);
        });
    }
}
