package com.buseiny.app;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.model.GlobalTask;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.DailyTaskRepository;
import com.buseiny.app.repository.GlobalTaskRepository;
import com.buseiny.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BuseinyApp {
    public static void main(String[] args) {
        SpringApplication.run(BuseinyApp.class, args);
    }
}
