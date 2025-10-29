package com.concurrent_web_crawler.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CrawlerApplication {

	public static void main(String[] args) {
        System.setProperty("server.port", System.getProperty("server.port", "4567"));
		SpringApplication.run(CrawlerApplication.class, args);
//		System.out.println("Hello, World!");
	}
}
