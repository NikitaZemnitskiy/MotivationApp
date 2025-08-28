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
import java.util.function.Function;
import java.util.stream.Collectors;
import com.buseiny.app.dto.HistoryDTO;
import com.buseiny.app.dto.RouletteDTO;
import java.time.format.DateTimeFormatter;

@Service
public class StateService {

    @Value("${app.dataFile}")
    private String dataFile;

    @Value("${app.timezone}")
    private String timezone;

    private final ObjectMapper mapper;
    private AppState state;
    private static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Random RNG = new Random();
    private record FixedDaily(String id, int reward, Function<DailyLog, Boolean> isDone) {}
    private static final List<FixedDaily> FIXED = List.of(
            new FixedDaily("nutrition", 2, d -> d != null && d.isNutritionDailyAwarded()),
            new FixedDaily("english", 1, d -> d != null && d.isEnglishDailyAwarded()),
            new FixedDaily("sport", 1, d -> d != null && d.isSportAwarded()),
            new FixedDaily("yoga", 1, d -> d != null && d.isYogaAwarded()),
            new FixedDaily("viet", 1, d -> d != null && d.isVietWordsAwarded())
    );


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

    private void processDayBoundariesIfNeeded() throws IOException {
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

    private void addBalance(int delta){
        var u = state.getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta)); // баланс не уходит ниже 0
    }

    private void addDailyWithRouletteBonus(String dailyId, int base){
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
        processDayBoundariesIfNeeded();
        var log = todayLog();
        log.setNutritionMinutes(log.getNutritionMinutes() + minutes);
        if (!log.isNutritionDailyAwarded() && log.getNutritionMinutes() >= 180){
            addDailyWithRouletteBonus("nutrition", 2);
            log.setNutritionDailyAwarded(true);
        }
        save();
    }

    public synchronized void addEnglishMinutes(int minutes) throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var log = todayLog();
        log.setEnglishMinutes(log.getEnglishMinutes() + minutes);
        if (!log.isEnglishDailyAwarded() && log.getEnglishMinutes() >= 60){
            addDailyWithRouletteBonus("english", 1);
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
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var log = todayLog();
        if (!log.isSportAwarded()){
            addDailyWithRouletteBonus("sport", 1);
            log.setSportAwarded(true);
            u.setSportStreak(u.getSportStreak()+1);
            if (u.getSportStreak() % 7 == 0){
                addBalance(7);
            }
        }
        save();
    }

    public synchronized void checkYoga() throws IOException {
        processDayBoundariesIfNeeded();
        var log = todayLog();
        if (!log.isYogaAwarded()){
            addDailyWithRouletteBonus("yoga", 1);
            log.setYogaAwarded(true);
        }
        save();
    }

    public synchronized void checkVietWords() throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var log = todayLog();
        if (!log.isVietWordsAwarded()){
            addDailyWithRouletteBonus("viet", 1);
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
        processDayBoundariesIfNeeded();
        for (var g : state.getGoals()){
            if (g.getId().equals(id)){
                if (!g.isCompleted()){
                    g.setCompletedAt(LocalDateTime.now(zone()));
                    int reward = g.getReward();
                    var rs = state.getAnna().getTodayRoulette();
                    if (rs != null
                            && LocalDate.now(zone()).equals(rs.getDate())
                            && rs.getEffect() == RouletteEffect.GOAL_X2
                            && g.getId().equals(rs.getGoalId())) {
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
        var opt = state.getShop().stream().filter(s -> s.getId().equals(id)).findFirst();
        if (opt.isEmpty()) return false;
        var item = opt.get();
        int cost = effectiveCostToday(item.getId(), item.getCost());
        if (u.getBalance() < cost) return false;
        u.setBalance(u.getBalance() - cost);
        u.getPurchases().add(new Purchase(item.getId(), item.getTitle(), cost, LocalDateTime.now(zone())));
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
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var todayKey = LocalDate.now(zone()).toString();
        var doneSet = u.getGenericDoneByDay().computeIfAbsent(todayKey, k -> new HashSet<>());
        if (doneSet.contains(taskId)) return; // уже отмечено
        var defOpt = state.getGenericDaily().stream().filter(d -> d.getId().equals(taskId)).findFirst();
        if (defOpt.isEmpty()) return;
        var def = defOpt.get();
        addDailyWithRouletteBonus("g:" + def.getId(), def.getDailyReward());
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

    public synchronized RouletteDTO getTodayRoulette() throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var today = LocalDate.now(zone());
        if (u.getTodayRoulette() != null && today.equals(u.getTodayRoulette().getDate())) {
            return toDTO(u.getTodayRoulette(), false, effectMessage(u.getTodayRoulette()));
        }
        var rs = new RouletteState();
        rs.setDate(today);
        return toDTO(rs, true, "Крути рулетку ✨");
    }

    public synchronized RouletteDTO spinRoulette() throws IOException {
        processDayBoundariesIfNeeded();
        var u = state.getAnna();
        var today = LocalDate.now(zone());
        if (u.getTodayRoulette() != null && today.equals(u.getTodayRoulette().getDate())) {
            return toDTO(u.getTodayRoulette(), false, "Сегодня уже крутили 💫");
        }
        int roll = RNG.nextInt(100);
        RouletteEffect eff;
        if (roll < 40) eff = RouletteEffect.DAILY_X2;
        else if (roll < 70) eff = RouletteEffect.GOAL_X2;
        else if (roll < 80) eff = RouletteEffect.BONUS_POINTS;
        else if (roll < 90) eff = RouletteEffect.SHOP_DISCOUNT_50;
        else eff = RouletteEffect.SHOP_FREE_UNDER_100;

        var rs = new RouletteState();
        rs.setDate(today);
        rs.setEffect(eff);

        switch (eff){
            case DAILY_X2 -> {
                List<String> candid = new ArrayList<>();
                Map<String,Integer> rewardById = new HashMap<>();
                for (var f : FIXED){
                    candid.add(f.id());
                    rewardById.put(f.id(), f.reward());
                }
                for (var gd : state.getGenericDaily()){
                    candid.add("g:" + gd.getId());
                    rewardById.put("g:" + gd.getId(), gd.getDailyReward());
                }
                String pick = candid.get(RNG.nextInt(candid.size()));
                rs.setDailyId(pick);
                rs.setDailyBaseReward(rewardById.get(pick));
            }
            case GOAL_X2 -> {
                var incomplete = state.getGoals().stream().filter(g -> g.getCompletedAt() == null).toList();
                if (!incomplete.isEmpty()){
                    var g = incomplete.get(RNG.nextInt(incomplete.size()));
                    rs.setGoalId(g.getId());
                } else {
                    rs.setEffect(RouletteEffect.BONUS_POINTS);
                    rs.setBonusPoints(1 + RNG.nextInt(5));
                    addBalance(rs.getBonusPoints());
                }
            }
            case BONUS_POINTS -> {
                int pts = 1 + RNG.nextInt(5);
                rs.setBonusPoints(pts);
                addBalance(pts);
            }
            case SHOP_DISCOUNT_50 -> {
                var items = state.getShop();
                if (!items.isEmpty()){
                    var it = items.get(RNG.nextInt(items.size()));
                    rs.setDiscountedShopId(it.getId());
                }
            }
            case SHOP_FREE_UNDER_100 -> {
                var items = state.getShop().stream().filter(i -> i.getCost() < 100).toList();
                if (!items.isEmpty()){
                    var it = items.get(RNG.nextInt(items.size()));
                    rs.setFreeShopId(it.getId());
                } else {
                    var all = state.getShop();
                    if (!all.isEmpty()){
                        var it = all.get(RNG.nextInt(all.size()));
                        rs.setDiscountedShopId(it.getId());
                        rs.setEffect(RouletteEffect.SHOP_DISCOUNT_50);
                    }
                }
            }
        }

        u.setTodayRoulette(rs);
        save();
        return toDTO(rs, false, effectMessage(rs));
    }

    private RouletteDTO toDTO(RouletteState rs, boolean canSpin, String msg){
        var dto = new RouletteDTO();
        dto.date = rs.getDate().toString();
        dto.effect = rs.getEffect();
        dto.dailyId = rs.getDailyId();
        dto.dailyBaseReward = rs.getDailyBaseReward();
        dto.goalId = rs.getGoalId();
        dto.bonusPoints = rs.getBonusPoints();
        dto.discountedShopId = rs.getDiscountedShopId();
        dto.freeShopId = rs.getFreeShopId();
        dto.canSpin = canSpin;
        dto.message = msg;
        return dto;
    }

    private String effectMessage(RouletteState rs){
        if (rs.getEffect() == null) return "";
        return switch (rs.getEffect()) {
            case DAILY_X2 -> "Сегодня дейлик \"" + prettyDaily(rs.getDailyId()) + "\" даёт x2, но штраф за пропуск (−" + rs.getDailyBaseReward() + ")!";
            case GOAL_X2 -> "Сегодня цель x2: " + goalTitle(rs.getGoalId());
            case BONUS_POINTS -> "Моментальный бонус: +" + rs.getBonusPoints();
            case SHOP_DISCOUNT_50 -> "−50% на покупку: " + shopTitle(rs.getDiscountedShopId());
            case SHOP_FREE_UNDER_100 -> "Бесплатно сегодня: " + shopTitle(rs.getFreeShopId());
        };
    }

    private String prettyDaily(String id){
        if (id == null) return "";
        return switch (id){
            case "nutrition" -> "Нутрициология";
            case "english" -> "Английский";
            case "sport" -> "Спорт";
            case "yoga" -> "Йога";
            case "viet" -> "5 вьет. слов";
            default -> {
                if (id.startsWith("g:")) yield genericTitle(id.substring(2));
                yield id;
            }
        };
    }

    private String goalTitle(String goalId){
        if (goalId == null) return "";
        return state.getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst().map(OneTimeGoal::getTitle).orElse(goalId);
    }

    private String shopTitle(String id){
        if (id == null) return "";
        return state.getShop().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().map(ShopItem::getTitle).orElse(id);
    }

    private String genericTitle(String gid){
        return state.getGenericDaily().stream()
                .filter(g -> g.getId().equals(gid))
                .findFirst().map(GenericDailyTaskDef::getTitle).orElse(gid);
    }

    private Map<String, List<HistoryDTO.Item>> buildStreakBonusesFixed() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();
        var daily = state.getAnna().getDaily();

        // Соберём все даты и отсортируем
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
                            .add(new HistoryDTO.Item("Стрик: Спорт (7 дней)", 7));
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
                            .add(new HistoryDTO.Item("Стрик: Английский (7 дней)", 7));
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
                            .add(new HistoryDTO.Item("Стрик: 5 вьет. слов (7 дней)", 7));
                }
            } else streak = 0;
        }
        return map;
    }

    // Стрики и очки для админских generic задач (по каждой задаче свой стрик)
    private Map<String, List<HistoryDTO.Item>> buildGenericDailyItemsAndBonuses() {
        Map<String, List<HistoryDTO.Item>> map = new HashMap<>();

        // соберём все даты (для которых есть отметки generic задач)
        Set<LocalDate> dateSet = new HashSet<>();
        for (var e : state.getAnna().getGenericDoneByDay().entrySet()) {
            dateSet.add(LocalDate.parse(e.getKey()));
        }
        List<LocalDate> dates = dateSet.stream().sorted().toList();

        // Для каждой задачи считаем стрик по дням
        for (var def : state.getGenericDaily()) {
            int streak = 0;
            for (LocalDate d : dates) {
                var set = state.getAnna().getGenericDoneByDay().getOrDefault(d.toString(), Collections.emptySet());
                boolean done = set.contains(def.getId());
                if (done) {
                    // ежедневная награда
                    map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                            .add(new HistoryDTO.Item("Ежедневно: " + def.getTitle(), def.getDailyReward()));
                    // стрик
                    if (def.isStreakEnabled()) {
                        streak++;
                        if (streak % 7 == 0) {
                            map.computeIfAbsent(d.toString(), k->new ArrayList<>())
                                    .add(new HistoryDTO.Item("Стрик: " + def.getTitle() + " (7 дней)", 7));
                        }
                    }
                } else {
                    if (def.isStreakEnabled()) streak = 0;
                }
            }
        }
        return map;
    }

    public synchronized HistoryDTO.DayHistory computeDayHistory(String dateStr) throws IOException {
        processDayBoundariesIfNeeded(); // на всякий случай

        LocalDate date = LocalDate.parse(dateStr);
        var u = state.getAnna();
        var daily = u.getDaily().get(dateStr);

        List<HistoryDTO.Item> items = new ArrayList<>();

        // Фиксированные ежедневные
        if (daily != null) {
            if (daily.isNutritionDailyAwarded()) items.add(new HistoryDTO.Item("Нутрициология 3 часа/день", 2));
            if (daily.isEnglishDailyAwarded())   items.add(new HistoryDTO.Item("Английский 1 час", 1));
            if (daily.isSportAwarded())          items.add(new HistoryDTO.Item("Спорт", 1));
            if (daily.isYogaAwarded())           items.add(new HistoryDTO.Item("Йога", 1));
            if (daily.isVietWordsAwarded())      items.add(new HistoryDTO.Item("5 вьетнамских слов", 1));
        }

        // Стриковые бонусы фиксированных задач
        var fixedBonuses = buildStreakBonusesFixed().getOrDefault(dateStr, List.of());
        items.addAll(fixedBonuses);

        // Generic daily + их стрики
        var genericMap = buildGenericDailyItemsAndBonuses();
        items.addAll(genericMap.getOrDefault(dateStr, List.of()));

        // Разовые цели (если завершены в этот день)
        for (var g : state.getGoals()) {
            if (g.getCompletedAt() != null && g.getCompletedAt().toLocalDate().equals(date)) {
                items.add(new HistoryDTO.Item("Достижение: " + g.getTitle(), g.getReward()));
            }
        }

        // (Опционально можно добавить недельный бонус/штраф, если хочешь — скажи, запишем транзакции и отрисуем здесь.)

        int total = items.stream().mapToInt(it -> it.points).sum();

        HistoryDTO.DayHistory dh = new HistoryDTO.DayHistory();
        dh.date = dateStr;
        dh.total = total;
        dh.items = items;
        return dh;
    }

    public synchronized HistoryDTO.MonthHistory computeMonthHistory(int year, int month) throws IOException {
        processDayBoundariesIfNeeded();

        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.plusMonths(1).minusDays(1);

        List<HistoryDTO.DayHistory> list = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            list.add(computeDayHistory(d.format(D)));
        }

        HistoryDTO.MonthHistory mh = new HistoryDTO.MonthHistory();
        mh.year = year;
        mh.month = month;
        mh.days = list;
        return mh;
    }

    // ===== Админ: баланс
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

    // ===== Админ: правка дня + пересчет
    public static class UpsertResult {
        public com.buseiny.app.dto.HistoryDTO.DayHistory day;
        public int newBalance;
    }

    public synchronized UpsertResult adminUpsertDayAndRecalc(com.buseiny.app.dto.AdminDayEditRequest req) throws IOException {
        if (req.date == null || req.date.isBlank()) throw new IllegalArgumentException("date required");
        var u = state.getAnna();

        // 1) правим DailyLog по дате
        var log = u.getDaily().computeIfAbsent(req.date, k -> new com.buseiny.app.model.DailyLog());
        if (req.nutritionMinutes != null) {
            log.setNutritionMinutes(Math.max(0, req.nutritionMinutes));
            log.setNutritionDailyAwarded(log.getNutritionMinutes() >= 180);
        }
        if (req.englishMinutes != null) {
            log.setEnglishMinutes(Math.max(0, req.englishMinutes));
            log.setEnglishDailyAwarded(log.getEnglishMinutes() >= 60);
        }
        if (req.sportDone != null)  log.setSportAwarded(req.sportDone);
        if (req.yogaDone != null)   log.setYogaAwarded(req.yogaDone);
        if (req.vietDone != null)   log.setVietWordsAwarded(req.vietDone);

        if (req.genericDoneIds != null) {
            // Полная замена набора выполненных generic-задач на дату
            u.getGenericDoneByDay().put(req.date, new java.util.HashSet<>(req.genericDoneIds));
        }

        save();

        // 2) тотальный пересчет баланса и стриков по всем дням
        recalcEverythingFromScratch();

        // 3) ответ
        UpsertResult out = new UpsertResult();
        out.day = computeDayHistory(req.date);
        out.newBalance = state.getAnna().getBalance();
        return out;
    }

    /**
     * Полный пересчет:
     * - Сбрасываем баланс, стрики и genericStreaks
     * - Проходим все даты по порядку:
     *    - считаем дневные очки (минутные и чекбоксы)
     *    - добавляем бонусы стриков по правилам (каждые 7 подряд +7)
     * - Добавляем разовые цели в день их выполнения
     * - Применяем недельный +14/-20 для каждой завершенной недели (после первой полной)
     * - Вычитаем покупки в день покупки
     * - Баланс не уходит ниже 0 (как в addBalance)
     */
    private void recalcEverythingFromScratch() throws IOException {
        var u = state.getAnna();
        // Сбрасываем
        u.setBalance(0);
        u.setSportStreak(0);
        u.setEnglishStreak(0);
        u.setVietWordsStreak(0);
        u.getGenericStreaks().clear();

        // Соберём все даты, где есть хоть что-то
        java.util.TreeSet<java.time.LocalDate> dates = new java.util.TreeSet<>();
        for (var k : u.getDaily().keySet()) dates.add(java.time.LocalDate.parse(k));
        for (var k : u.getGenericDoneByDay().keySet()) dates.add(java.time.LocalDate.parse(k));
        for (var g : state.getGoals()) {
            if (g.getCompletedAt() != null) dates.add(g.getCompletedAt().toLocalDate());
        }
        for (var p : u.getPurchases()) {
            if (p.getPurchasedAt() != null) dates.add(p.getPurchasedAt().toLocalDate());
        }
        if (dates.isEmpty()) { save(); return; }

        // Порог для недельного расчета
        java.time.LocalDate firstFullWeekStart = firstFullWeekStart();
        java.time.LocalDate minDate = dates.first();
        java.time.LocalDate maxDate = java.time.LocalDate.now(zone()); // до сегодня

        // Идём по дням
        java.time.LocalDate d = minDate;
        int sportStreak = 0, engStreak = 0, vietStreak = 0;
        java.util.Map<String,Integer> genericStreaks = new java.util.HashMap<>();

        while (!d.isAfter(maxDate)) {
            String key = d.toString();
            var log = u.getDaily().get(key);

            // Дневные награды
            if (log != null) {
                if (log.isNutritionDailyAwarded()) addBalance(2);
                if (log.isEnglishDailyAwarded())   addBalance(1);
                if (log.isSportAwarded())          addBalance(1);
                if (log.isYogaAwarded())           addBalance(1);
                if (log.isVietWordsAwarded())      addBalance(1);
            }

            // Бонусы стриков для фиксированных задач
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

            // Generic daily + стрики
            var doneSet = u.getGenericDoneByDay().getOrDefault(key, java.util.Collections.emptySet());
            if (!doneSet.isEmpty()) {
                for (var def : state.getGenericDaily()) {
                    if (doneSet.contains(def.getId())) {
                        addBalance(def.getDailyReward());
                        if (def.isStreakEnabled()) {
                            int s = genericStreaks.getOrDefault(def.getId(), 0) + 1;
                            genericStreaks.put(def.getId(), s);
                            if (s % 7 == 0) addBalance(7);
                        }
                    }
                }
            }

            // Разовые цели этим днём
            for (var g : state.getGoals()) {
                if (g.getCompletedAt() != null && g.getCompletedAt().toLocalDate().equals(d)) {
                    addBalance(g.getReward());
                }
            }

            // Покупки этим днём
            for (var p : u.getPurchases()) {
                if (p.getPurchasedAt() != null && p.getPurchasedAt().toLocalDate().equals(d)) {
                    u.setBalance(Math.max(0, u.getBalance() - p.getCostSnapshot()));
                }
            }

            // Применяем недельный +14/-20 по завершению недели (в понедельник новой недели)
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
