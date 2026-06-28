package dev.daniel.fiscalflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FiscalFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FiscalFlowApplication.class, args);
    }
}
