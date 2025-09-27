package com.buseiny.app.service;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.model.History;
import com.buseiny.app.model.HistoryType;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.DailyTaskRepository;
import com.buseiny.app.repository.HistoryRepository;
import com.buseiny.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanService {

    private final UserRepository userRepository;
    private final DailyTaskRepository taskRepository;
    private final HistoryRepository historyRepository;

    @Transactional
    public void resetDailyProgress() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<DailyTask> tasks = taskRepository.findAll();

        for (DailyTask task : tasks) {
            User user = task.getUser();

            boolean missedYesterday = task.getLastCompletedDate() == null
                    || (!task.getLastCompletedDate().isEqual(yesterday) && !task.getLastCompletedDate().isEqual(today));

            if (missedYesterday && task.getDailyPenalty() > 0) {
                boolean alreadyPenalized = historyRepository.existsByUserAndTypeAndDateAndReason(
                        user, HistoryType.PENALTY, today, "Penalty for task: " + task.getTitle()
                );

                if (!alreadyPenalized) {
                    user.setBalance(user.getBalance() - task.getDailyPenalty());

                    History history = new History();
                    history.setUser(user);
                    history.setAmount(-task.getDailyPenalty());
                    history.setType(HistoryType.PENALTY);
                    history.setDate(today);
                    history.setReason("Penalty for task: " + task.getTitle());
                    historyRepository.save(history);
                }
            }

            if (task.getLastCompletedDate() == null || !task.getLastCompletedDate().isEqual(today)) {
                System.out.println("Очистка дня выполнения для " + task.getTitle());
                task.setCurrentDoneToday(0);
            }

            if(missedYesterday){
                System.out.println("Очистка стрика для " + task.getTitle());
                task.setCurrentStreak(0);
            }
        }

        userRepository.saveAll(userRepository.findAll());
        taskRepository.saveAll(tasks);
    }


    @Transactional
    public void processWeeklyTasks() {
        LocalDate today = LocalDate.now();

        // Запускаем только если понедельник
        if (!today.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            System.out.println("Джоба решила запуститься");
            return;
        }

        List<DailyTask> tasks = taskRepository.findAll();

        for (DailyTask task : tasks) {
            User user = task.getUser();

            if (task.isHasWeeklyNorm()) {
                boolean success = task.getCurrentDoneOnThisWeek() >= task.getWeeklyNorm();

                if (success && task.getWeaklyReward() > 0) {
                    boolean alreadyRewarded = historyRepository.existsByUserAndTypeAndDateAndReason(
                            user, HistoryType.WEEKLY_REWARD, today, "Weekly reward for task: " + task.getTitle()
                    );

                    if (!alreadyRewarded) {
                        user.setBalance(user.getBalance() + task.getWeaklyReward());

                        History history = new History();
                        history.setUser(user);
                        history.setAmount(task.getWeaklyReward());
                        history.setType(HistoryType.WEEKLY_REWARD);
                        history.setDate(today);
                        history.setReason("Weekly reward for task: " + task.getTitle());
                        historyRepository.save(history);
                    }
                }

                if (!success && task.getWeaklyPenalty() > 0) {
                    boolean alreadyPenalized = historyRepository.existsByUserAndTypeAndDateAndReason(
                            user, HistoryType.WEEKLY_PENALTY, today, "Weekly penalty for task: " + task.getTitle()
                    );

                    if (!alreadyPenalized) {
                        user.setBalance(user.getBalance() - task.getWeaklyPenalty());

                        History history = new History();
                        history.setUser(user);
                        history.setAmount(-task.getWeaklyPenalty());
                        history.setType(HistoryType.WEEKLY_PENALTY);
                        history.setDate(today);
                        history.setReason("Weekly penalty for task: " + task.getTitle());
                        historyRepository.save(history);
                    }
                }

                // обнуляем недельный прогресс
                task.setCurrentDoneOnThisWeek(0);
            }
        }

        userRepository.saveAll(userRepository.findAll());
        taskRepository.saveAll(tasks);
    }


    // Запуск каждый день в 00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void resetAtMidnight() {
        resetDailyProgress();
    }

    // Запуск один раз при старте приложения
    @EventListener(ApplicationReadyEvent.class)
    public void resetOnStartup() {
        resetDailyProgress();
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void runWeeklyProcessing() {
        processWeeklyTasks();
    }
}
