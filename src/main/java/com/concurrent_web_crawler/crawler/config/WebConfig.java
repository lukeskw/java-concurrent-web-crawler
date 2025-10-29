package com.concurrent_web_crawler.crawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WebConfig {

    @Value("${server.port:4567}")

    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public RestTemplate restTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(factory);
    }

    @Bean
    public URI baseUri(@Value("${crawler.base-url}") String baseUrl) {
       // String value = baseUrlEnv != null ? baseUrlEnv : System.getenv("BASE_URL");
       // if (value == null || value.isBlank()) {
       //     throw new IllegalStateException("BASE_URL não definida. Ex: BASE_URL=http://hiring.axreng.com/");
        //}
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("BASE_URL não definida. Ex: BASE_URL=http://hiring.axreng.com/");
        }
        return URI.create(normalizeBase(baseUrl));
    }

    private static String normalizeBase(String url) {
        String s = url.trim();
        if (!s.endsWith("/")) s = s + "/";
        return s;
    }
}
