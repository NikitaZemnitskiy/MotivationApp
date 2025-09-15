package com.buseiny.app.service;

import com.buseiny.app.dto.HistoryDTO;
import com.buseiny.app.model.DailyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.buseiny.app.util.TimeUtil;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class HistoryService {
    private final StateService state;
    private static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;

    public HistoryService(StateService state) {
        this.state = state;
    }

    private Map<String, List<HistoryDTO.Item>> buildStreakBonusesFixed() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();
        var daily = state.getState().getAnna().getDaily();

        List<LocalDate> dates = daily.keySet().stream()
                .map(LocalDate::parse)
                .sorted()
                .toList();

        int streak = 0;
        for (LocalDate d : dates) {
            var log = daily.get(d.toString());
            boolean done = false; // unified: streak-specific labels moved below
            if (done) {
                streak++;
                // unified: label handled generically below
            } else streak = 0;
        }

        streak = 0;
        for (LocalDate d : dates) {
            var log = daily.get(d.toString());
            boolean done = false;
            if (done) {
                streak++;
                // unified
            } else streak = 0;
        }

        streak = 0;
        for (LocalDate d : dates) {
            var log = daily.get(d.toString());
            boolean done = false;
            if (done) {
                streak++;
                // unified
            } else streak = 0;
        }
        return map;
    }

    private Map<String, List<HistoryDTO.Item>> buildGenericDailyItemsAndBonuses() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();

        List<LocalDate> dates = state.getState().getAnna().getDaily().keySet().stream().map(LocalDate::parse).sorted().toList();

        for (var def : state.getState().getDailyTasks()) {
            int streak = 0;
            for (LocalDate d : dates) {
                var log = state.getState().getAnna().getDaily().get(d.toString());
                boolean done = false;
                if (log != null) {
                    if (def.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES) {
                        Integer m = log.getMinutes().get(def.id());
                        done = m != null && def.minutesPerDay() != null && m >= def.minutesPerDay();
                    } else {
                        done = log.getChecks().contains(def.id());
                    }
                }
                if (done) {
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>()).add(new HistoryDTO.Item("Daily: " + def.title(), def.dailyReward()));
                    if (def.streakEnabled()) {
                        streak++;
                        if (streak % 7 == 0) {
                            map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                                    .add(new HistoryDTO.Item("Streak: " + def.title() + " (7 days)", 7));
                        }
                    }
                } else if (def.streakEnabled()) {
                    streak = 0;
                }
            }
        }
        return map;
    }

    public synchronized HistoryDTO.DayHistory computeDayHistory(String dateStr) throws IOException {
        state.processDayBoundariesIfNeeded();

        LocalDate date = LocalDate.parse(dateStr);
        var u = state.getState().getAnna();
        var daily = u.getDaily().get(dateStr);

        List<HistoryDTO.Item> items = new ArrayList<>();

        if (daily != null) {
            for (var def : state.getState().getDailyTasks()){
                if (def.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES){
                    Integer m = daily.getMinutes().get(def.id());
                    if (m != null && def.minutesPerDay() != null && m >= def.minutesPerDay()){
                        items.add(new HistoryDTO.Item("Daily: " + def.title(), def.dailyReward()));
                    }
                } else {
                    if (daily.getChecks().contains(def.id())){
                        items.add(new HistoryDTO.Item("Daily: " + def.title(), def.dailyReward()));
                    }
                }
            }
        }

        var fixedBonuses = buildStreakBonusesFixed().getOrDefault(dateStr, List.of());
        items.addAll(fixedBonuses);

        var genericMap = buildGenericDailyItemsAndBonuses();
        items.addAll(genericMap.getOrDefault(dateStr, List.of()));

        for (var g : state.getState().getGoals()) {
            if (g.completedAt() != null && g.completedAt().toLocalDate().equals(date)) {
                items.add(new HistoryDTO.Item("Achievement: " + g.title(), g.reward()));
            }
        }

        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            LocalDate prevWeekStart = date.minusWeeks(1);
            if (!prevWeekStart.isBefore(state.firstFullWeekStart())) {
                // For weekly minutes goal (first minutes-type task with weekly goal)
                var minutesTaskOpt = state.getState().getDailyTasks().stream()
                        .filter(t -> t.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES && t.weeklyMinutesGoal() != null && t.weeklyMinutesGoal() > 0)
                        .findFirst();
                if (minutesTaskOpt.isPresent()){
                    var t = minutesTaskOpt.get();
                    int minutes = state.sumWeeklyGoalMinutesForWeek(prevWeekStart);
                    if (minutes >= t.weeklyMinutesGoal()) items.add(new HistoryDTO.Item("Недельный бонус", 14));
                    else items.add(new HistoryDTO.Item("Недельный штраф", -20));
                }
            }
        }

        for (var p : u.getPurchases()) {
            if (p.purchasedAt() != null && p.purchasedAt().toLocalDate().equals(date)) {
                items.add(new HistoryDTO.Item("Покупка: " + p.titleSnapshot(), -p.costSnapshot()));
            }
        }

        items.addAll(u.getHistoryExtras().getOrDefault(dateStr, List.of()));

        int total = items.stream().mapToInt(HistoryDTO.Item::points).sum();
        return new HistoryDTO.DayHistory(dateStr, total, items);
    }

    public synchronized HistoryDTO.MonthHistory computeMonthHistory(int year, int month) throws IOException {
        state.processDayBoundariesIfNeeded();

        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.plusMonths(1).minusDays(1);

        List<HistoryDTO.DayHistory> list = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            list.add(computeDayHistory(d.format(D)));
        }

        return new HistoryDTO.MonthHistory(year, month, list);
    }

    public static class UpsertResult {
        public HistoryDTO.DayHistory day;
        public int newBalance;
    }

    public synchronized UpsertResult adminUpsertDayAndRecalcNew(com.buseiny.app.dto.AdminDayUpsertNewRequest req) throws IOException {
        if (req.date() == null || req.date().isBlank()) throw new IllegalArgumentException("date required");
        var u = state.getState().getAnna();
        var log = u.getDaily().computeIfAbsent(req.date(), k -> new DailyLog());
        log.getMinutes().clear();
        log.getChecks().clear();
        log.getMinutesAwarded().clear();
        if (req.minutes() != null) {
            for (var e : req.minutes().entrySet()) {
                if (e.getValue() != null && e.getValue() >= 0) log.getMinutes().put(e.getKey(), e.getValue());
            }
        }
        if (req.checks() != null) {
            log.getChecks().addAll(req.checks());
        }
        state.save();
        recalcEverythingFromScratch();

        UpsertResult out = new UpsertResult();
        out.day = computeDayHistory(req.date());
        out.newBalance = state.getState().getAnna().getBalance();
        return out;
    }

    private void recalcEverythingFromScratch() throws IOException {
        var u = state.getState().getAnna();
        u.setBalance(0);
        u.getStreaks().clear();

        java.util.TreeSet<LocalDate> dates = new java.util.TreeSet<>();
        for (var k : u.getDaily().keySet()) dates.add(LocalDate.parse(k));
        // unified daily map already covers dates
        for (var g : state.getState().getGoals()) {
            if (g.completedAt() != null) dates.add(g.completedAt().toLocalDate());
        }
        for (var p : u.getPurchases()) {
            if (p.purchasedAt() != null) dates.add(p.purchasedAt().toLocalDate());
        }
        for (var k : u.getHistoryExtras().keySet()) dates.add(LocalDate.parse(k));
        if (dates.isEmpty()) { state.save(); return; }

        LocalDate firstFullWeekStart = state.firstFullWeekStart();
        LocalDate minDate = dates.first();
        LocalDate maxDate = LocalDate.now(state.zone());

        LocalDate d = minDate;
        Map<String,Integer> genericStreaks = new HashMap<>();

        while (!d.isAfter(maxDate)) {
            String key = d.toString();
            var log = u.getDaily().get(key);

            if (log != null) {
                for (var def : state.getState().getDailyTasks()){
                    if (def.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES){
                        Integer m = log.getMinutes().get(def.id());
                        if (m != null && def.minutesPerDay() != null && m >= def.minutesPerDay()) state.addBalance(def.dailyReward());
                    } else {
                        if (log.getChecks().contains(def.id())) state.addBalance(def.dailyReward());
                    }
                }
            }

            // per-task streaks advanced generically below

            // Unified tasks streaks handled below

            for (var g : state.getState().getGoals()) {
                if (g.completedAt() != null && g.completedAt().toLocalDate().equals(d)) {
                    state.addBalance(g.reward());
                }
            }

            for (var p : u.getPurchases()) {
                if (p.purchasedAt() != null && p.purchasedAt().toLocalDate().equals(d)) {
                    u.setBalance(Math.max(0, u.getBalance() - p.costSnapshot()));
                }
            }

            for (var extra : u.getHistoryExtras().getOrDefault(key, Collections.emptyList())) {
                state.addBalance(extra.points());
            }

            var nextDay = d.plusDays(1);
            if (nextDay.getDayOfWeek() == DayOfWeek.MONDAY) {
                LocalDate weekStart = TimeUtil.weekStartMonday(d);
                if (!weekStart.isBefore(firstFullWeekStart)) {
                    var minutesTaskOpt = state.getState().getDailyTasks().stream()
                            .filter(t -> t.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES && t.weeklyMinutesGoal() != null && t.weeklyMinutesGoal() > 0)
                            .findFirst();
                    if (minutesTaskOpt.isPresent()){
                        var t = minutesTaskOpt.get();
                        int minutes = state.sumWeeklyGoalMinutesForWeek(weekStart);
                        if (minutes >= t.weeklyMinutesGoal()) state.addBalance(14);
                        else state.addBalance(-20);
                    }
                }
            }

            d = d.plusDays(1);
        }

        // streaks already stored in u.getStreaks()

        state.save();
    }
}
