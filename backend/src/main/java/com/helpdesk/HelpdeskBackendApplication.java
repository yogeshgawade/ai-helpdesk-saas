package com.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelpdeskBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpdeskBackendApplication.class, args);
	}

}
