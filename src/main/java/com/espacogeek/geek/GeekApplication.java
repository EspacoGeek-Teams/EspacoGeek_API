package com.espacogeek.geek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeekApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeekApplication.class, args);
	}

}
