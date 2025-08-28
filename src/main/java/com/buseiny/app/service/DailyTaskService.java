package com.buseiny.app.service;

import com.buseiny.app.model.DailyLog;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;

@Service
@Slf4j
public class DailyTaskService {
    private final StateService state;

    public DailyTaskService(StateService state) {
        this.state = state;
    }

    public synchronized void addNutritionMinutes(int minutes) throws IOException {
        state.processDayBoundariesIfNeeded();
        DailyLog logEntry = state.todayLog();
        logEntry.setNutritionMinutes(logEntry.getNutritionMinutes() + minutes);
        if (!logEntry.isNutritionDailyAwarded() && logEntry.getNutritionMinutes() >= 180) {
            state.addDailyWithRouletteBonus("nutrition", 2);
            logEntry.setNutritionDailyAwarded(true);
            log.info("Nutrition daily completed, bonus applied");
        }
        state.save();
    }

    public synchronized void addEnglishMinutes(int minutes) throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog logEntry = state.todayLog();
        logEntry.setEnglishMinutes(logEntry.getEnglishMinutes() + minutes);
        if (!logEntry.isEnglishDailyAwarded() && logEntry.getEnglishMinutes() >= 60) {
            state.addDailyWithRouletteBonus("english", 1);
            logEntry.setEnglishDailyAwarded(true);
            u.setEnglishStreak(u.getEnglishStreak() + 1);
            log.info("English streak is now {}", u.getEnglishStreak());
            if (u.getEnglishStreak() % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }

    public synchronized void checkSport() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog logEntry = state.todayLog();
        if (!logEntry.isSportAwarded()) {
            state.addDailyWithRouletteBonus("sport", 1);
            logEntry.setSportAwarded(true);
            u.setSportStreak(u.getSportStreak() + 1);
            log.info("Sport streak is now {}", u.getSportStreak());
            if (u.getSportStreak() % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }

    public synchronized void checkYoga() throws IOException {
        state.processDayBoundariesIfNeeded();
        DailyLog logEntry = state.todayLog();
        if (!logEntry.isYogaAwarded()) {
            state.addDailyWithRouletteBonus("yoga", 1);
            logEntry.setYogaAwarded(true);
            log.info("Yoga completed today");
        }
        state.save();
    }

    public synchronized void checkVietWords() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog logEntry = state.todayLog();
        if (!logEntry.isVietWordsAwarded()) {
            state.addDailyWithRouletteBonus("viet", 1);
            logEntry.setVietWordsAwarded(true);
            u.setVietWordsStreak(u.getVietWordsStreak() + 1);
            log.info("Vietnamese words streak is now {}", u.getVietWordsStreak());
            if (u.getVietWordsStreak() % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }

    public synchronized void checkGenericTask(String taskId) throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        var todayKey = LocalDate.now(state.zone()).toString();
        var doneSet = u.getGenericDoneByDay().computeIfAbsent(todayKey, k -> new java.util.HashSet<>());
        if (doneSet.contains(taskId)) return;
        var defOpt = state.getState().getGenericDaily().stream().filter(d -> d.getId().equals(taskId)).findFirst();
        if (defOpt.isEmpty()) return;
        var def = defOpt.get();
        state.addDailyWithRouletteBonus("g:" + def.getId(), def.getDailyReward());
        doneSet.add(taskId);

        if (def.isStreakEnabled()) {
            int streak = u.getGenericStreaks().getOrDefault(taskId, 0) + 1;
            u.getGenericStreaks().put(taskId, streak);
            log.info("Generic task {} streak is now {}", taskId, streak);
            if (streak % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }
}
