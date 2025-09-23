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

    @Bean
    CommandLineRunner init(UserRepository userRepo,
                           DailyTaskRepository dailyTaskRepo,
                           GlobalTaskRepository globalTaskRepo) {
        return args -> {
            if (userRepo.findByUsername("anna").isEmpty()) {
                User anna = new User();
                anna.setUsername("anna");
                anna.setDisplayName("Anna");
                anna.setBalance(120);
                userRepo.save(anna);

                User test = new User();
                test.setUsername("test");
                test.setDisplayName("Test");
                test.setBalance(5);
                userRepo.save(test);

                DailyTask t1 = new DailyTask();
                t1.setTitle("Проснуться в 8 утра");
                t1.setDescription("Проснуться до 8:00");
                t1.setDailyReward(10);
                t1.setChecked(true);
                t1.setUser(anna);
                t1.setWeeklyNorm(5);
                dailyTaskRepo.save(t1);

                DailyTask t5 = new DailyTask();
                t5.setTitle("Радовать Никиту");
                t5.setDescription("радовать меня");
                t5.setDailyReward(1);
                t5.setDailyPenalty(5);
                t5.setChecked(true);
                t5.setStreakEnabled(true);
                t5.setStreakMultiplied(2);
                t5.setUser(anna);
                t5.setWeeklyNorm(5);
                dailyTaskRepo.save(t5);

                DailyTask t2 = new DailyTask();
                t2.setTitle("Изучать английский 30 мин");
                t2.setDescription("Минимум 30 минут");
                t2.setDailyReward(20);
                t2.setWeeklyNorm(240);
                t2.setMinutesGoal(60);

                t2.setStreakEnabled(true);
                t2.setStreakMultiplied(2);
                t2.setUser(anna);
                dailyTaskRepo.save(t2);

                DailyTask t3 = new DailyTask();
                t3.setTitle("Сделать напиток");
                t3.setDailyReward(5);
                t3.setCountGoal(5);
                t3.setStreakEnabled(true);
                t3.setStreakMultiplied(2);
                t3.setWeeklyNorm(10);
                t3.setHasWeeklyNorm(true);
                t3.setUser(anna);
                dailyTaskRepo.save(t3);

                GlobalTask g1 = new GlobalTask();
                g1.setTitle("Подарок-сюрприз");
                g1.setDescription("Купить маленький сюрприз");
                g1.setUser(anna);
                g1.setReward(500);
                globalTaskRepo.save(g1);
            }
        };
    }
}
