package com.concurrent_web_crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CrawlerApplication {

	void main(String[] args) {
        System.setProperty("server.port", System.getProperty("server.port", "4567"));
		SpringApplication.run(CrawlerApplication.class, args);
	}
}
