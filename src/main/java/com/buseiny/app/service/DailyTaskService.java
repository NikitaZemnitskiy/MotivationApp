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

    // legacy-specific helpers removed; use addMinutesByTaskId and checkGenericTask

    public synchronized void checkGenericTask(String taskId) throws IOException {
        checkGeneric(taskId, rewardFor(taskId), streakEnabled(taskId));
    }

    public synchronized void addMinutesByTaskId(String taskId, int minutes) throws IOException {
        state.processDayBoundariesIfNeeded();
        var defOpt = state.getState().getDailyTasks().stream().filter(d -> d.id().equals(taskId)).findFirst();
        if (defOpt.isEmpty()) return;
        var def = defOpt.get();
        if (def.kind() != com.buseiny.app.model.DailyTaskKind.MINUTES) return;
        int threshold = def.minutesPerDay() == null ? Integer.MAX_VALUE : def.minutesPerDay();
        int baseReward = def.dailyReward();
        DailyLog logEntry = state.todayLog();
        var map = logEntry.getMinutes();
        map.put(taskId, map.getOrDefault(taskId, 0) + minutes);
        if (!logEntry.getMinutesAwarded().contains(taskId) && map.get(taskId) >= threshold) {
            state.addDailyWithRouletteBonus(taskId, baseReward);
            logEntry.getMinutesAwarded().add(taskId);
            if (def.streakEnabled()) {
                var u = state.getState().getAnna();
                int s = u.getStreaks().getOrDefault(taskId, 0) + 1;
                u.getStreaks().put(taskId, s);
                if (s % 7 == 0) state.addBalance(7);
            }
        }
        state.save();
    }

    private int rewardFor(String taskId){
        return state.getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(taskId))
                .findFirst().map(com.buseiny.app.model.DailyTaskDef::dailyReward).orElse(1);
    }

    private boolean streakEnabled(String taskId){
        return state.getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(taskId))
                .findFirst().map(com.buseiny.app.model.DailyTaskDef::streakEnabled).orElse(false);
    }

    

    private void checkGeneric(String id, int baseReward, boolean streak) throws IOException {
        state.processDayBoundariesIfNeeded();
        DailyLog logEntry = state.todayLog();
        if (logEntry.getChecks().contains(id)) return;
        state.addDailyWithRouletteBonus(id, baseReward);
        logEntry.getChecks().add(id);
        if (streak){
            var u = state.getState().getAnna();
            int s = u.getStreaks().getOrDefault(id, 0) + 1;
            u.getStreaks().put(id, s);
            if (s % 7 == 0) state.addBalance(7);
        }
        state.save();
    }
}
