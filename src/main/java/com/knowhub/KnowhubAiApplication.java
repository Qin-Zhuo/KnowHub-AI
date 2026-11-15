package com.knowhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KnowhubAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowhubAiApplication.class, args);
    }

}
