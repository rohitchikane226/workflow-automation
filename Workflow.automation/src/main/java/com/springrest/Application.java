package com.springrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAutoConfiguration
@EnableScheduling
@SpringBootApplication(exclude = {
	    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
	})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
