package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
@EnableCaching
public class NexoraBackendApplication {
	public static void main(String[] args) { SpringApplication.run(NexoraBackendApplication.class, args);}
}
