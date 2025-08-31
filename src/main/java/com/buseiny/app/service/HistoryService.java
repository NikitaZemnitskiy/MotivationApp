package com.buseiny.app.service;

import com.buseiny.app.dto.AdminDayEditRequest;
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
            boolean done = log != null && log.isSportAwarded();
            if (done) {
                streak++;
                if (streak % 7 == 0) {
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Streak: Sport (7 days)", 7));
                }
            } else streak = 0;
        }

        streak = 0;
        for (LocalDate d : dates) {
            var log = daily.get(d.toString());
            boolean done = log != null && log.isEnglishDailyAwarded();
            if (done) {
                streak++;
                if (streak % 7 == 0) {
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Streak: English (7 days)", 7));
                }
            } else streak = 0;
        }

        streak = 0;
        for (LocalDate d : dates) {
            var log = daily.get(d.toString());
            boolean done = log != null && log.isVietWordsAwarded();
            if (done) {
                streak++;
                if (streak % 7 == 0) {
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Streak: 5 Viet words (7 days)", 7));
                }
            } else streak = 0;
        }
        return map;
    }

    private Map<String, List<HistoryDTO.Item>> buildGenericDailyItemsAndBonuses() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();

        Set<LocalDate> dateSet = new HashSet<>();
        for (var e : state.getState().getAnna().getGenericDoneByDay().entrySet()) {
            dateSet.add(LocalDate.parse(e.getKey()));
        }
        List<LocalDate> dates = dateSet.stream().sorted().toList();

        for (var def : state.getState().getGenericDaily()) {
            int streak = 0;
            for (LocalDate d : dates) {
                var set = state.getState().getAnna().getGenericDoneByDay().getOrDefault(d.toString(), Collections.emptySet());
                boolean done = set.contains(def.id());
                if (done) {
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Daily: " + def.title(), def.dailyReward()));
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
            if (daily.isNutritionDailyAwarded()) items.add(new HistoryDTO.Item("Nutrition 3h/day", 2));
            if (daily.isEnglishDailyAwarded())   items.add(new HistoryDTO.Item("English 1h", 1));
            if (daily.isSportAwarded())          items.add(new HistoryDTO.Item("Sport", 1));
            if (daily.isYogaAwarded())           items.add(new HistoryDTO.Item("Yoga", 1));
            if (daily.isVietWordsAwarded())      items.add(new HistoryDTO.Item("5 Vietnamese words", 1));
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
                int minutes = state.sumNutritionMinutesForWeek(prevWeekStart);
                if (minutes >= 1080) items.add(new HistoryDTO.Item("Недельный бонус", 14));
                else items.add(new HistoryDTO.Item("Недельный штраф", -20));
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

    public synchronized UpsertResult adminUpsertDayAndRecalc(AdminDayEditRequest req) throws IOException {
        if (req.date() == null || req.date().isBlank()) throw new IllegalArgumentException("date required");
        var u = state.getState().getAnna();

        var log = u.getDaily().computeIfAbsent(req.date(), k -> new DailyLog());
        if (req.nutritionMinutes() != null) {
            log.setNutritionMinutes(Math.max(0, req.nutritionMinutes()));
            log.setNutritionDailyAwarded(log.getNutritionMinutes() >= 180);
        }
        if (req.englishMinutes() != null) {
            log.setEnglishMinutes(Math.max(0, req.englishMinutes()));
            log.setEnglishDailyAwarded(log.getEnglishMinutes() >= 60);
        }
        if (req.sportDone() != null)  log.setSportAwarded(req.sportDone());
        if (req.yogaDone() != null)   log.setYogaAwarded(req.yogaDone());
        if (req.vietDone() != null)   log.setVietWordsAwarded(req.vietDone());

        if (req.genericDoneIds() != null) {
            u.getGenericDoneByDay().put(req.date(), new HashSet<>(req.genericDoneIds()));
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
        u.setSportStreak(0);
        u.setEnglishStreak(0);
        u.setVietWordsStreak(0);
        u.getGenericStreaks().clear();

        java.util.TreeSet<LocalDate> dates = new java.util.TreeSet<>();
        for (var k : u.getDaily().keySet()) dates.add(LocalDate.parse(k));
        for (var k : u.getGenericDoneByDay().keySet()) dates.add(LocalDate.parse(k));
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
        int sportStreak = 0, engStreak = 0, vietStreak = 0;
        Map<String,Integer> genericStreaks = new HashMap<>();

        while (!d.isAfter(maxDate)) {
            String key = d.toString();
            var log = u.getDaily().get(key);

            if (log != null) {
                if (log.isNutritionDailyAwarded()) state.addBalance(2);
                if (log.isEnglishDailyAwarded())   state.addBalance(1);
                if (log.isSportAwarded())          state.addBalance(1);
                if (log.isYogaAwarded())           state.addBalance(1);
                if (log.isVietWordsAwarded())      state.addBalance(1);
            }

            boolean sportDone = log != null && log.isSportAwarded();
            if (sportDone) {
                sportStreak++;
                if (sportStreak % 7 == 0) state.addBalance(7);
            } else sportStreak = 0;

            boolean engDone = log != null && log.isEnglishDailyAwarded();
            if (engDone) {
                engStreak++;
                if (engStreak % 7 == 0) state.addBalance(7);
            } else engStreak = 0;

            boolean vietDone = log != null && log.isVietWordsAwarded();
            if (vietDone) {
                vietStreak++;
                if (vietStreak % 7 == 0) state.addBalance(7);
            } else vietStreak = 0;

            var doneSet = u.getGenericDoneByDay().getOrDefault(key, Collections.emptySet());
            if (!doneSet.isEmpty()) {
                for (var def : state.getState().getGenericDaily()) {
                    if (doneSet.contains(def.id())) {
                        state.addBalance(def.dailyReward());
                        if (def.streakEnabled()) {
                            int s = genericStreaks.getOrDefault(def.id(), 0) + 1;
                            genericStreaks.put(def.id(), s);
                            if (s % 7 == 0) state.addBalance(7);
                        }
                    }
                }
            }

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
                    int minutes = state.sumNutritionMinutesForWeek(weekStart);
                    if (minutes >= 1080) state.addBalance(14);
                    else state.addBalance(-20);
                }
            }

            d = d.plusDays(1);
        }

        u.setSportStreak(sportStreak);
        u.setEnglishStreak(engStreak);
        u.setVietWordsStreak(vietStreak);
        u.setGenericStreaks(genericStreaks);

        state.save();
    }
}
