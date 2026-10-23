package com.smartpruefung.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SmartPruefungBackEndApplication {

	@Autowired
	StartUpConfiguration startUpConfiguration;

	public static void main(String[] args) {
		SpringApplication.run(SmartPruefungBackEndApplication.class, args);
	}

	@Bean
	public CommandLineRunner CommandLineRunnerBean() {
		return (args) -> {
			startUpConfiguration.Init();
			for (String arg : args) {
				System.out.println(arg);
			}
		};
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
