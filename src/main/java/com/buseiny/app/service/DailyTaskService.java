package com.buseiny.app.service;

import com.buseiny.app.model.DailyLog;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class DailyTaskService {
    private final StateService state;

    public DailyTaskService(StateService state) {
        this.state = state;
    }

    public synchronized void addNutritionMinutes(int minutes) throws IOException {
        state.processDayBoundariesIfNeeded();
        DailyLog log = state.todayLog();
        log.setNutritionMinutes(log.getNutritionMinutes() + minutes);
        if (!log.isNutritionDailyAwarded() && log.getNutritionMinutes() >= 180) {
            state.addDailyWithRouletteBonus("nutrition", 2);
            log.setNutritionDailyAwarded(true);
        }
        state.save();
    }

    public synchronized void addEnglishMinutes(int minutes) throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog log = state.todayLog();
        log.setEnglishMinutes(log.getEnglishMinutes() + minutes);
        if (!log.isEnglishDailyAwarded() && log.getEnglishMinutes() >= 60) {
            state.addDailyWithRouletteBonus("english", 1);
            log.setEnglishDailyAwarded(true);
            u.setEnglishStreak(u.getEnglishStreak() + 1);
            if (u.getEnglishStreak() % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }

    public synchronized void checkSport() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog log = state.todayLog();
        if (!log.isSportAwarded()) {
            state.addDailyWithRouletteBonus("sport", 1);
            log.setSportAwarded(true);
            u.setSportStreak(u.getSportStreak() + 1);
            if (u.getSportStreak() % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }

    public synchronized void checkYoga() throws IOException {
        state.processDayBoundariesIfNeeded();
        DailyLog log = state.todayLog();
        if (!log.isYogaAwarded()) {
            state.addDailyWithRouletteBonus("yoga", 1);
            log.setYogaAwarded(true);
        }
        state.save();
    }

    public synchronized void checkVietWords() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        DailyLog log = state.todayLog();
        if (!log.isVietWordsAwarded()) {
            state.addDailyWithRouletteBonus("viet", 1);
            log.setVietWordsAwarded(true);
            u.setVietWordsStreak(u.getVietWordsStreak() + 1);
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
            if (streak % 7 == 0) {
                state.addBalance(7);
            }
        }
        state.save();
    }
}
