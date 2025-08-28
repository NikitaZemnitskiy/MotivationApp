package com.buseiny.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.buseiny.app.model.*;
import com.buseiny.app.util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StateService {

    @Value("${app.dataFile}")
    private String dataFile;

    @Value("${app.timezone}")
    private String timezone;

    private final ObjectMapper mapper;
    private AppState state;

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
            new OneTimeGoal("sunrise", "Увидеть рассвет", 6),
            new OneTimeGoal("meet-vn-girl", "Познакомиться с вьетнамкой", 15),
            new OneTimeGoal("date-vn-girl", "Встретиться с вьетнамкой", 20)
        );
        state.setGoals(new ArrayList<>(goals));
    }

    private void seedShop(){
        var shop = List.of(
            new ShopItem("lazy-day", "День тюлень (без осуждения)", 100),
            new ShopItem("walk", "Прогулка на выбор", 20),
            new ShopItem("nikita-sport", "Занятие спортом Никиты", 30),
            new ShopItem("nikita-shopping", "Поездка на шопинг Никиты", 50),
            new ShopItem("coffee-out", "Поход в кофейню (или кофе домой)", 30),
            new ShopItem("coffee-sweet", "Кофе от Никиты с конфетой и комплиментами", 10),
            new ShopItem("day-trip", "Поездка куда хочешь на целый день", 250),
            new ShopItem("movie-night", "Вечер кино (с ужина и до сна)", 75),
            new ShopItem("no-gadgets", "День без гаджетов только с любимкой", 200),
            new ShopItem("secret-gift", "Секретный подарок", 300)
        );
        state.setShop(new ArrayList<>(shop));
    }

    public synchronized AppState getState(){ return state; }
    public ZoneId zone(){ return TimeUtil.zone(timezone); }

    public synchronized void save() throws IOException {
        mapper.writeValue(new File(dataFile), state);
    }

    // --- Helpers ---
    private String todayKey(){
        return LocalDate.now(zone()).toString();
    }

    private DailyLog todayLog(){
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
        // обработать все завершённые недели между lastProcessed и currentWeekStart
        while (lastProcessed.isBefore(currentWeekStart)){
            // неделя [lastProcessed .. lastProcessed+6] завершена
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

    private void addBalance(int delta){
        var u = state.getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta)); // баланс не уходит ниже 0
    }

    // --- Public API used by controllers ---
    public synchronized Map<String,Object> status() throws IOException {
        processWeekIfNeeded();
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
            todayGenericDone.put(def.getId(), doneSet.contains(def.getId()));
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

    public synchronized void addNutritionMinutes(int minutes) throws IOException {
        var log = todayLog();
        log.setNutritionMinutes(log.getNutritionMinutes() + minutes);
        if (!log.isNutritionDailyAwarded() && log.getNutritionMinutes() >= 180){
            addBalance(2);
            log.setNutritionDailyAwarded(true);
        }
        save();
    }

    public synchronized void addEnglishMinutes(int minutes) throws IOException {
        var u = state.getAnna();
        var log = todayLog();
        log.setEnglishMinutes(log.getEnglishMinutes() + minutes);
        if (!log.isEnglishDailyAwarded() && log.getEnglishMinutes() >= 60){
            addBalance(1);
            log.setEnglishDailyAwarded(true);
            // streak +7 каждый 7
            u.setEnglishStreak(u.getEnglishStreak()+1);
            if (u.getEnglishStreak() % 7 == 0){
                addBalance(7);
            }
        }
        save();
    }

    public synchronized void checkSport() throws IOException {
        var u = state.getAnna();
        var log = todayLog();
        if (!log.isSportAwarded()){
            addBalance(1);
            log.setSportAwarded(true);
            u.setSportStreak(u.getSportStreak()+1);
            if (u.getSportStreak() % 7 == 0){
                addBalance(7);
            }
        }
        save();
    }

    public synchronized void checkYoga() throws IOException {
        var log = todayLog();
        if (!log.isYogaAwarded()){
            addBalance(1);
            log.setYogaAwarded(true);
        }
        save();
    }

    public synchronized void checkVietWords() throws IOException {
        var u = state.getAnna();
        var log = todayLog();
        if (!log.isVietWordsAwarded()){
            addBalance(1);
            log.setVietWordsAwarded(true);
            u.setVietWordsStreak(u.getVietWordsStreak()+1);
            if (u.getVietWordsStreak() % 7 == 0){
                addBalance(7);
            }
        }
        save();
    }

    public synchronized void resetStreaksIfMissedYesterday(){
        // Стрики обнуляются при пропуске. Для простоты обнуляем на входе в новый день,
        // если вчера не было отметки. (Минимальная реализация)
    }

    public synchronized boolean completeGoal(String id) throws IOException {
        for (var g : state.getGoals()){
            if (g.getId().equals(id)){
                if (!g.isCompleted()){
                    g.setCompletedAt(LocalDateTime.now(zone()));
                    addBalance(g.getReward());
                    save();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized boolean purchase(String id) throws IOException {
        var u = state.getAnna();
        var opt = state.getShop().stream().filter(s -> s.getId().equals(id)).findFirst();
        if (opt.isEmpty()) return false;
        var item = opt.get();
        if (u.getBalance() < item.getCost()) return false;
        u.setBalance(u.getBalance() - item.getCost());
        u.getPurchases().add(new Purchase(item.getId(), item.getTitle(), item.getCost(), LocalDateTime.now(zone())));
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

    public synchronized void checkGenericTask(String taskId) throws IOException {
        var u = state.getAnna();
        var todayKey = LocalDate.now(zone()).toString();
        var doneSet = u.getGenericDoneByDay().computeIfAbsent(todayKey, k -> new HashSet<>());
        if (doneSet.contains(taskId)) return; // уже отмечено
        var defOpt = state.getGenericDaily().stream().filter(d -> d.getId().equals(taskId)).findFirst();
        if (defOpt.isEmpty()) return;
        var def = defOpt.get();
        addBalance(def.getDailyReward());
        doneSet.add(taskId);

        if (def.isStreakEnabled()){
            int streak = u.getGenericStreaks().getOrDefault(taskId, 0) + 1;
            u.getGenericStreaks().put(taskId, streak);
            if (streak % 7 == 0){
                addBalance(7);
            }
        }
        save();
    }
}
