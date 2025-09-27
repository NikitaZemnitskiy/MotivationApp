package com.buseiny.app.service;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.model.GlobalTask;
import com.buseiny.app.model.HistoryType;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.DailyTaskRepository;
import com.buseiny.app.repository.GlobalTaskRepository;
import com.buseiny.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class TaskService {

    private final UserRepository userRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final GlobalTaskRepository globalTaskRepository;
    private final HistoryService historyService;


    @Transactional
    public void completeDailyTask(Long taskId, int count) {
        DailyTask task = dailyTaskRepository.findById(taskId).orElseThrow();
        User user = task.getUser();
        LocalDate today = LocalDate.now();

        //Check is task checked And already done today
        if (task.getLastCompletedDate() != null && task.getLastCompletedDate().equals(today)) {
            return;
        }

        boolean isTaskDoneNow = isDoneNow(task, count);

        if(isTaskDoneNow){
            setStreak(task);
            boolean isStreakEnabled = task.isStreakEnabled() && task.getCurrentStreak() >= 7;
            int reward = isStreakEnabled ? task.getDailyReward() * task.getStreakMultiplied() : task.getDailyReward();
            historyService.addHistory(user, reward,task.getTitle() + (isStreakEnabled?"со стриком":""), true, HistoryType.DAILY_COMPLETE);
            task.setLastCompletedDate(today);
            user.setBalance(user.getBalance() + reward);
        }


        task.setCurrentDoneToday(task.getCurrentDoneToday() + count);
        task.setCurrentDoneOnThisWeek(task.getCurrentDoneOnThisWeek() + count);


        dailyTaskRepository.save(task);
        userRepository.save(user);
    }

    @Transactional
    public void completeGlobalTask(Long taskId) {
        GlobalTask task = globalTaskRepository.findById(taskId).orElseThrow();
        User user = task.getUser();

        if (!task.isCompleted()) {
            task.setCompleted(true);
            historyService.addHistory(user, task.getReward(),task.getTitle(), false, HistoryType.GLOBAL_COMPLETE);
            user.setBalance(user.getBalance() + task.getReward());
            globalTaskRepository.save(task);
            userRepository.save(user);
        }
    }

    private boolean isDoneNow(DailyTask task, int count) {
        if (task.isChecked()) {
            return true;
        }
        if (task.getCountGoal() > 0
                && task.getCurrentDoneToday() < task.getCountGoal()
                && task.getCurrentDoneToday() + count >= task.getCountGoal()
        ) {
            return true;
        }
        if (task.getMinutesGoal() > 0
                && task.getCurrentDoneToday() + count >= task.getMinutesGoal()
                && task.getCurrentDoneToday() < task.getMinutesGoal()
        ) {
            return true;
        }
        return false;
    }

    private void setStreak(DailyTask task) {
        LocalDate today = LocalDate.now();

        if (task.getLastCompletedDate() != null) {
            if (task.getLastCompletedDate().equals(today.minusDays(1))) {
                task.setCurrentStreak(task.getCurrentStreak() + 1);
            } else if (!task.getLastCompletedDate().equals(today)) {
                task.setCurrentStreak(1); // streak resets
            }
        } else {
            task.setCurrentStreak(1); // first completion
        }
    }

    public DailyTask addTask(DailyTask task, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setUser(user);

        // Проверка типа задания: только одно поле должно быть заполнено
        int typeCount = 0;
        if (task.getMinutesGoal() > 0) typeCount++;
        if (task.getCountGoal() > 0) typeCount++;
        if(task.getMinutesGoal()<=0 && task.getCountGoal() <= 0){
           task.setChecked(true);
            typeCount++;
        }
        if (typeCount != 1) {
            throw new IllegalArgumentException("Можно выбрать только один тип задания: checked, minutesGoal или countGoal");
        }

        return dailyTaskRepository.save(task);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
