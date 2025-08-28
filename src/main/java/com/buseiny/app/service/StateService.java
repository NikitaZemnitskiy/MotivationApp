package com.buseiny.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.buseiny.app.model.*;
import com.buseiny.app.util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import com.buseiny.app.dto.HistoryDTO;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class StateService {

    @Value("${app.dataFile}")
    private String dataFile;

    @Value("${app.timezone}")
    private String timezone;

    private final ObjectMapper mapper;
    private AppState state;
    private static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;



    public StateService(){
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public synchronized void init() throws IOException {
        File f = new File(dataFile);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        if (f.exists()) {
            state = mapper.readValue(f, AppState.class);
        } else {
            state = freshState();
            save();
        }
        if (state.getAnna().getAvatarUrl() == null) {
            state.getAnna().setAvatarUrl("/assets/avatar.png");
        }
        if (state.getGoals() == null || state.getGoals().isEmpty()){
            seedGoals();
        }
        if (state.getShop() == null || state.getShop().isEmpty()){
            seedShop();
        }
        save();
    }

    private AppState freshState(){
        AppState s = new AppState();
        s.setInstalledAt(LocalDateTime.now(TimeUtil.zone(timezone)));
        s.setLastProcessedWeekStart(TimeUtil.weekStartMonday(LocalDate.now(TimeUtil.zone(timezone))));
        UserState u = new UserState();
        u.setUsername("Anna");
        u.setBalance(0);
        u.setAvatarUrl("/assets/avatar.png");
        s.setAnna(u);
        return s;
    }

    private void seedGoals(){
        var goals = List.of(
            new OneTimeGoal("sunrise", "See the sunrise", 6),
            new OneTimeGoal("meet-vn-girl", "Meet a Vietnamese girl", 15),
            new OneTimeGoal("date-vn-girl", "Date a Vietnamese girl", 20)
        );
        state.setGoals(new ArrayList<>(goals));
    }

    private void seedShop(){
        var shop = List.of(
            new ShopItem("lazy-day", "Lazy day (no judgment)", 100),
            new ShopItem("walk", "Walk of choice", 20),
            new ShopItem("nikita-sport", "Nikita sports session", 30),
            new ShopItem("nikita-shopping", "Shopping trip for Nikita", 50),
            new ShopItem("coffee-out", "Coffee outing (or coffee at home)", 30),
            new ShopItem("coffee-sweet", "Coffee from Nikita with candy and compliments", 10),
            new ShopItem("day-trip", "Day trip anywhere you want", 250),
            new ShopItem("movie-night", "Movie night (from dinner to bedtime)", 75),
            new ShopItem("no-gadgets", "Gadget-free day with your loved one", 200),
            new ShopItem("secret-gift", "Secret gift", 300)
        );
        state.setShop(new ArrayList<>(shop));
    }

    public synchronized AppState getState(){ return state; }
    public ZoneId zone(){ return TimeUtil.zone(timezone); }

    public synchronized void save() throws IOException {
        mapper.writeValue(new File(dataFile), state);
        log.debug("State persisted to {}", dataFile);
    }

    // --- Helpers ---
    private String todayKey(){
        return LocalDate.now(zone()).toString();
    }

    DailyLog todayLog(){
        var daily = state.getAnna().getDaily();
        return daily.computeIfAbsent(todayKey(), k -> new DailyLog());
    }

    private int sumNutritionMinutesForWeek(LocalDate weekStart){
        LocalDate d = weekStart;
        int sum = 0;
        for (int i=0;i<7;i++){
            String key = d.toString();
            var log = state.getAnna().getDaily().get(key);
            if (log != null) sum += log.getNutritionMinutes();
            d = d.plusDays(1);
        }
        return sum;
    }

    private LocalDate firstFullWeekStart(){
        var installed = state.getInstalledAt().toLocalDate();
        return TimeUtil.firstMondayAfter(installed);
    }

    public synchronized void processWeekIfNeeded() throws IOException {
        LocalDate today = LocalDate.now(zone());
        LocalDate currentWeekStart = TimeUtil.weekStartMonday(today);
        LocalDate lastProcessed = state.getLastProcessedWeekStart();
        if (lastProcessed == null){
            state.setLastProcessedWeekStart(currentWeekStart);
            save();
            return;
        }
        // process all completed weeks between lastProcessed and currentWeekStart
        while (lastProcessed.isBefore(currentWeekStart)){
            // week [lastProcessed .. lastProcessed+6] is complete
            LocalDate weekStart = lastProcessed;
            if (!weekStart.isBefore(firstFullWeekStart())){
                int minutes = sumNutritionMinutesForWeek(weekStart);
                if (minutes >= 1080){ // 18*60
                    addBalance(14);
                } else {
                    addBalance(-20);
                }
            }
            lastProcessed = lastProcessed.plusWeeks(1);
        }
        if (!lastProcessed.equals(state.getLastProcessedWeekStart())){
            state.setLastProcessedWeekStart(lastProcessed);
            save();
        }
    }

    synchronized void processDayBoundariesIfNeeded() throws IOException {
        processWeekIfNeeded();
        var u = state.getAnna();
        var today = LocalDate.now(zone());
        var rs = u.getTodayRoulette();
        if (rs == null) return;
        if (rs.getEffect() == RouletteEffect.DAILY_X2
                && !today.equals(rs.getDate())
                && !rs.isDailyPenaltyApplied()) {
            var dailyDone = isDailyDone(rs.getDate(), rs.getDailyId());
            if (!dailyDone) {
                addBalance(-Math.abs(rs.getDailyBaseReward()));
            }
            rs.setDailyPenaltyApplied(true);
            save();
        }
    }

    private boolean isDailyDone(LocalDate date, String dailyId){
        var log = state.getAnna().getDaily().get(date.toString());
        if (dailyId == null) return false;
        switch (dailyId){
            case "nutrition": return log != null && log.isNutritionDailyAwarded();
            case "english": return log != null && log.isEnglishDailyAwarded();
            case "sport": return log != null && log.isSportAwarded();
            case "yoga": return log != null && log.isYogaAwarded();
            case "viet": return log != null && log.isVietWordsAwarded();
            default:
                if (dailyId.startsWith("g:")){
                    var set = state.getAnna().getGenericDoneByDay()
                            .getOrDefault(date.toString(), Collections.emptySet());
                    return set.contains(dailyId.substring(2));
                }
                return false;
        }
    }

    synchronized void addBalance(int delta){
        var u = state.getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta)); // balance never drops below 0
        log.info("Balance adjusted by {} to {}", delta, u.getBalance());
    }

    void addDailyWithRouletteBonus(String dailyId, int base){
        int mult = isRouletteDailyToday(dailyId) ? 2 : 1;
        addBalance(base * mult);
    }

    private boolean isRouletteDailyToday(String dailyId){
        var u = state.getAnna();
        var rs = u.getTodayRoulette();
        var today = LocalDate.now(zone());
        return rs != null
                && today.equals(rs.getDate())
                && rs.getEffect() == RouletteEffect.DAILY_X2
                && dailyId.equals(rs.getDailyId());
    }

    private int effectiveCostToday(String itemId, int baseCost){
        var rs = state.getAnna().getTodayRoulette();
        if (rs == null || !LocalDate.now(zone()).equals(rs.getDate())) return baseCost;
        if (itemId.equals(rs.getFreeShopId())) return 0;
        if (itemId.equals(rs.getDiscountedShopId())) return Math.max(0, baseCost / 2);
        return baseCost;
    }

    // --- Public API used by controllers ---
    public synchronized Map<String,Object> status() throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var today = LocalDate.now(zone());
        var weekStart = TimeUtil.weekStartMonday(today);
        var weekEndInstant = TimeUtil.weekEndInstant(today, zone());

        // aggregate week minutes
        int weekMinutes = 0;
        for (int i=0;i<7;i++){
            var d = weekStart.plusDays(i).toString();
            var log = u.getDaily().get(d);
            if (log != null) weekMinutes += log.getNutritionMinutes();
        }

        var todayLog = u.getDaily().getOrDefault(today.toString(), new DailyLog());

        Map<String, Boolean> todayGenericDone = new HashMap<>();
        var doneSet = u.getGenericDoneByDay().getOrDefault(today.toString(), new HashSet<>());
        for (var def : state.getGenericDaily()){
            todayGenericDone.put(def.id(), doneSet.contains(def.id()));
        }

        Map<String, Integer> genericStreaks = new HashMap<>(u.getGenericStreaks());

        Map<String,Object> map = new HashMap<>();
        map.put("username", u.getUsername());
        map.put("avatarUrl", u.getAvatarUrl());
        map.put("balance", u.getBalance());
        map.put("todayNutritionMinutes", todayLog.getNutritionMinutes());
        map.put("todayNutritionAwarded", todayLog.isNutritionDailyAwarded());
        map.put("todayEnglishMinutes", todayLog.getEnglishMinutes());
        map.put("todayEnglishAwarded", todayLog.isEnglishDailyAwarded());
        map.put("todaySportDone", todayLog.isSportAwarded());
        map.put("sportStreak", u.getSportStreak());
        map.put("todayYogaDone", todayLog.isYogaAwarded());
        map.put("todayVietDone", todayLog.isVietWordsAwarded());
        map.put("vietStreak", u.getVietWordsStreak());
        map.put("englishStreak", u.getEnglishStreak());
        map.put("weekNutritionMinutes", weekMinutes);
        map.put("weekGoalMinutes", 1080);
        map.put("secondsUntilWeekEndEpoch", weekEndInstant.getEpochSecond());
        map.put("currentWeekStart", weekStart.toString());
        map.put("goals", state.getGoals());
        map.put("shop", state.getShop());
        map.put("genericDaily", state.getGenericDaily());
        map.put("todayGenericDone", todayGenericDone);
        map.put("genericStreaks", genericStreaks);
        return map;
    }


    public synchronized void resetStreaksIfMissedYesterday(){
        // Streaks reset when a day is missed. Minimal placeholder implementation.
    }

    public synchronized boolean completeGoal(String id) throws IOException {
        processDayBoundariesIfNeeded();
        for (int i = 0; i < state.getGoals().size(); i++) {
            var g = state.getGoals().get(i);
            if (g.id().equals(id)) {
                if (!g.isCompleted()) {
                    var updated = new OneTimeGoal(g.id(), g.title(), g.reward(), LocalDateTime.now(zone()));
                    state.getGoals().set(i, updated);
                    int reward = g.reward();
                    var rs = state.getAnna().getTodayRoulette();
                    if (rs != null
                            && LocalDate.now(zone()).equals(rs.getDate())
                            && rs.getEffect() == RouletteEffect.GOAL_X2
                            && g.id().equals(rs.getGoalId())) {
                        reward *= 2;
                    }
                    addBalance(reward);
                    save();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized boolean purchase(String id) throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var opt = state.getShop().stream().filter(s -> s.id().equals(id)).findFirst();
        if (opt.isEmpty()) return false;
        var item = opt.get();
        int cost = effectiveCostToday(item.id(), item.cost());
        if (u.getBalance() < cost) return false;
        u.setBalance(u.getBalance() - cost);
        u.getPurchases().add(new Purchase(item.id(), item.title(), cost, LocalDateTime.now(zone())));
        save();
        return true;
    }

    // --- Admin ---
    public synchronized List<ShopItem> setShop(List<ShopItem> items) throws IOException {
        state.setShop(new ArrayList<>(items));
        save();
        return state.getShop();
    }
    public synchronized List<OneTimeGoal> setGoals(List<OneTimeGoal> items) throws IOException {
        state.setGoals(new ArrayList<>(items));
        save();
        return state.getGoals();
    }
    public synchronized List<GenericDailyTaskDef> setGenericDaily(List<GenericDailyTaskDef> items) throws IOException {
        state.setGenericDaily(new ArrayList<>(items));
        save();
        return state.getGenericDaily();
    }


    private Map<String, List<HistoryDTO.Item>> buildStreakBonusesFixed() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();
        var daily = state.getAnna().getDaily();

        // Collect and sort all dates
        List<LocalDate> dates = daily.keySet().stream()
                .map(LocalDate::parse)
                .sorted()
                .toList();

        // SPORT
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
        // ENGLISH
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
        // VIET WORDS
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

    // Streaks and points for admin-defined generic tasks
    private Map<String, List<HistoryDTO.Item>> buildGenericDailyItemsAndBonuses() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();

        // collect all dates with generic task marks
        Set<LocalDate> dateSet = new HashSet<>();
        for (var e : state.getAnna().getGenericDoneByDay().entrySet()) {
            dateSet.add(LocalDate.parse(e.getKey()));
        }
        List<LocalDate> dates = dateSet.stream().sorted().toList();

        // compute streak per task by day
        for (var def : state.getGenericDaily()) {
            int streak = 0;
            for (LocalDate d : dates) {
                var set = state.getAnna().getGenericDoneByDay().getOrDefault(d.toString(), Collections.emptySet());
                boolean done = set.contains(def.id());
                if (done) {
                    // daily reward
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Daily: " + def.title(), def.dailyReward()));
                    // streak bonus
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
        processDayBoundariesIfNeeded(); // just in case

        LocalDate date = LocalDate.parse(dateStr);
        var u = state.getAnna();
        var daily = u.getDaily().get(dateStr);

        List<HistoryDTO.Item> items = new ArrayList<>();

        // Fixed daily tasks
        if (daily != null) {
            if (daily.isNutritionDailyAwarded()) items.add(new HistoryDTO.Item("Nutrition 3h/day", 2));
            if (daily.isEnglishDailyAwarded())   items.add(new HistoryDTO.Item("English 1h", 1));
            if (daily.isSportAwarded())          items.add(new HistoryDTO.Item("Sport", 1));
            if (daily.isYogaAwarded())           items.add(new HistoryDTO.Item("Yoga", 1));
            if (daily.isVietWordsAwarded())      items.add(new HistoryDTO.Item("5 Vietnamese words", 1));
        }

        // Streak bonuses for fixed tasks
        var fixedBonuses = buildStreakBonusesFixed().getOrDefault(dateStr, List.of());
        items.addAll(fixedBonuses);

        // Generic daily tasks and their streaks
        var genericMap = buildGenericDailyItemsAndBonuses();
        items.addAll(genericMap.getOrDefault(dateStr, List.of()));

        // One-time goals completed on this day
        for (var g : state.getGoals()) {
            if (g.completedAt() != null && g.completedAt().toLocalDate().equals(date)) {
                items.add(new HistoryDTO.Item("Achievement: " + g.title(), g.reward()));
            }
        }

        int total = items.stream().mapToInt(HistoryDTO.Item::points).sum();
        return new HistoryDTO.DayHistory(dateStr, total, items);
    }

    public synchronized HistoryDTO.MonthHistory computeMonthHistory(int year, int month) throws IOException {
        processDayBoundariesIfNeeded();

        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.plusMonths(1).minusDays(1);

        List<HistoryDTO.DayHistory> list = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            list.add(computeDayHistory(d.format(D)));
        }

        return new HistoryDTO.MonthHistory(year, month, list);
    }

    // ===== Admin: balance
    public synchronized int adminAddBalance(int delta) throws IOException {
        var u = state.getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta));
        save();
        return u.getBalance();
    }
    public synchronized int adminSetBalance(int value) throws IOException {
        var u = state.getAnna();
        u.setBalance(Math.max(0, value));
        save();
        return u.getBalance();
    }

    // ===== Admin: day edit and recomputation
    public static class UpsertResult {
        public com.buseiny.app.dto.HistoryDTO.DayHistory day;
        public int newBalance;
    }

    public synchronized UpsertResult adminUpsertDayAndRecalc(com.buseiny.app.dto.AdminDayEditRequest req) throws IOException {
        if (req.date() == null || req.date().isBlank()) throw new IllegalArgumentException("date required");
        var u = state.getAnna();

        // 1) update DailyLog for the given date
        var log = u.getDaily().computeIfAbsent(req.date(), k -> new com.buseiny.app.model.DailyLog());
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
            // Replace the set of completed generic tasks for this date
            u.getGenericDoneByDay().put(req.date(), new java.util.HashSet<>(req.genericDoneIds()));
        }

        save();

        // 2) recompute balance and streaks across all days
        recalcEverythingFromScratch();

        // 3) return result
        UpsertResult out = new UpsertResult();
        out.day = computeDayHistory(req.date());
        out.newBalance = state.getAnna().getBalance();
        return out;
    }

    /**
     * Full recomputation:
     * - reset balance, streaks and genericStreaks
     * - iterate all dates in order:
     *   - compute daily points (minutes and checkboxes)
     *   - add streak bonuses (every 7 consecutive days +7)
     * - add one-time goals on the day they were completed
     * - apply weekly +14/-20 for each finished week (after the first full week)
     * - subtract purchases on the day they occur
     * - ensure balance never drops below zero
     */
    private void recalcEverythingFromScratch() throws IOException {
        var u = state.getAnna();
        // Reset state
        u.setBalance(0);
        u.setSportStreak(0);
        u.setEnglishStreak(0);
        u.setVietWordsStreak(0);
        u.getGenericStreaks().clear();

        // Collect all dates with any activity
        java.util.TreeSet<java.time.LocalDate> dates = new java.util.TreeSet<>();
        for (var k : u.getDaily().keySet()) dates.add(java.time.LocalDate.parse(k));
        for (var k : u.getGenericDoneByDay().keySet()) dates.add(java.time.LocalDate.parse(k));
        for (var g : state.getGoals()) {
            if (g.completedAt() != null) dates.add(g.completedAt().toLocalDate());
        }
        for (var p : u.getPurchases()) {
            if (p.purchasedAt() != null) dates.add(p.purchasedAt().toLocalDate());
        }
        if (dates.isEmpty()) { save(); return; }

        // Threshold for weekly calculation
        java.time.LocalDate firstFullWeekStart = firstFullWeekStart();
        java.time.LocalDate minDate = dates.first();
        java.time.LocalDate maxDate = java.time.LocalDate.now(zone()); // up to today

        // Iterate day by day
        java.time.LocalDate d = minDate;
        int sportStreak = 0, engStreak = 0, vietStreak = 0;
        java.util.Map<String,Integer> genericStreaks = new java.util.HashMap<>();

        while (!d.isAfter(maxDate)) {
            String key = d.toString();
            var log = u.getDaily().get(key);

            // Daily rewards
            if (log != null) {
                if (log.isNutritionDailyAwarded()) addBalance(2);
                if (log.isEnglishDailyAwarded())   addBalance(1);
                if (log.isSportAwarded())          addBalance(1);
                if (log.isYogaAwarded())           addBalance(1);
                if (log.isVietWordsAwarded())      addBalance(1);
            }

            // Streak bonuses for fixed tasks
            boolean sportDone = log != null && log.isSportAwarded();
            if (sportDone) {
                sportStreak++;
                if (sportStreak % 7 == 0) addBalance(7);
            } else sportStreak = 0;

            boolean engDone = log != null && log.isEnglishDailyAwarded();
            if (engDone) {
                engStreak++;
                if (engStreak % 7 == 0) addBalance(7);
            } else engStreak = 0;

            boolean vietDone = log != null && log.isVietWordsAwarded();
            if (vietDone) {
                vietStreak++;
                if (vietStreak % 7 == 0) addBalance(7);
            } else vietStreak = 0;

            // Generic daily tasks and streaks
            var doneSet = u.getGenericDoneByDay().getOrDefault(key, java.util.Collections.emptySet());
            if (!doneSet.isEmpty()) {
                for (var def : state.getGenericDaily()) {
                    if (doneSet.contains(def.id())) {
                        addBalance(def.dailyReward());
                        if (def.streakEnabled()) {
                            int s = genericStreaks.getOrDefault(def.id(), 0) + 1;
                            genericStreaks.put(def.id(), s);
                            if (s % 7 == 0) addBalance(7);
                        }
                    }
                }
            }

            // One-time goals on this day
            for (var g : state.getGoals()) {
                if (g.completedAt() != null && g.completedAt().toLocalDate().equals(d)) {
                    addBalance(g.reward());
                }
            }

            // Purchases on this day
            for (var p : u.getPurchases()) {
                if (p.purchasedAt() != null && p.purchasedAt().toLocalDate().equals(d)) {
                    u.setBalance(Math.max(0, u.getBalance() - p.costSnapshot()));
                }
            }

            // Apply weekly +14/-20 at the start of each new week (Monday)
            var nextDay = d.plusDays(1);
            if (nextDay.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                java.time.LocalDate weekStart = com.buseiny.app.util.TimeUtil.weekStartMonday(d);
                if (!weekStart.isBefore(firstFullWeekStart)) {
                    int minutes = sumNutritionMinutesForWeek(weekStart);
                    if (minutes >= 1080) addBalance(14);
                    else addBalance(-20);
                }
            }

            d = d.plusDays(1);
        }

        u.setSportStreak(sportStreak);
        u.setEnglishStreak(engStreak);
        u.setVietWordsStreak(vietStreak);
        u.setGenericStreaks(genericStreaks);

        save();
    }

}
