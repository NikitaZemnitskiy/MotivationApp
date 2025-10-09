package com.buseiny.app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BuseinyApp {
    public static void main(String[] args) {
        SpringApplication.run(BuseinyApp.class, args);
    }
}
