package com.project.devcommunity;

import com.project.devcommunity.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DevcommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevcommunityApplication.class, args);
	}

}
