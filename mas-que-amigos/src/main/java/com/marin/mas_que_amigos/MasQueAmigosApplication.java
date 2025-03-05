package com.marin.mas_que_amigos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = "com.marin.mas_que_amigos")
public class MasQueAmigosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MasQueAmigosApplication.class, args);
	}

}
