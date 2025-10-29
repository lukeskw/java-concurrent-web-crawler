package com.concurrent_web_crawler.crawler.repository;

import com.concurrent_web_crawler.crawler.model.CrawlRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawlRequestRepository extends JpaRepository<CrawlRequest, Long> {
    Optional<CrawlRequest> findByKeywordNormalized(String keywordNormalized);
}
