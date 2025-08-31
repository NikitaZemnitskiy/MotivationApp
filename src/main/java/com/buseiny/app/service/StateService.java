package com.buseiny.app.service;

import com.buseiny.app.dto.HistoryDTO;
import com.buseiny.app.model.*;
import com.buseiny.app.repository.StateRepository;
import com.buseiny.app.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.*;

@Service
@Slf4j
public class StateService {

    @Value("${app.timezone}")
    private String timezone;

    private final StateRepository repo;
    public StateService(StateRepository repo){
        this.repo = repo;
    }

    public synchronized AppState getState(){ return repo.get(); }
    public ZoneId zone(){ return TimeUtil.zone(timezone); }

    public synchronized void save() throws IOException {
        repo.save();
    }

    // --- Helpers ---
    private String todayKey(){
        return LocalDate.now(zone()).toString();
    }

    DailyLog todayLog(){
        var daily = getState().getAnna().getDaily();
        return daily.computeIfAbsent(todayKey(), k -> new DailyLog());
    }

    int sumNutritionMinutesForWeek(LocalDate weekStart){
        LocalDate d = weekStart;
        int sum = 0;
        for (int i=0;i<7;i++){
            String key = d.toString();
            var log = getState().getAnna().getDaily().get(key);
            if (log != null) sum += log.getNutritionMinutes();
            d = d.plusDays(1);
        }
        return sum;
    }

    LocalDate firstFullWeekStart(){
        var installed = getState().getInstalledAt().toLocalDate();
        return TimeUtil.firstMondayAfter(installed);
    }

    public synchronized void processWeekIfNeeded() throws IOException {
        LocalDate today = LocalDate.now(zone());
        LocalDate currentWeekStart = TimeUtil.weekStartMonday(today);
        LocalDate lastProcessed = getState().getLastProcessedWeekStart();
        if (lastProcessed == null){
            getState().setLastProcessedWeekStart(currentWeekStart);
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
        if (!lastProcessed.equals(getState().getLastProcessedWeekStart())){
            getState().setLastProcessedWeekStart(lastProcessed);
            save();
        }
    }

    synchronized void processDayBoundariesIfNeeded() throws IOException {
        processWeekIfNeeded();
        var u = getState().getAnna();
        var today = LocalDate.now(zone());
        var rs = u.getTodayRoulette();
        if (rs == null) return;
        if (rs.getEffect() == RouletteEffect.DAILY_X2
                && !today.equals(rs.getDate())
                && !rs.isDailyPenaltyApplied()) {
            var dailyDone = isDailyDone(rs.getDate(), rs.getDailyId());
            if (!dailyDone) {
                int pen = -Math.abs(rs.getDailyBaseReward());
                addBalance(pen);
                addHistory(today, "Штраф за пропуск: " + prettyDaily(rs.getDailyId()), pen);
            }
            rs.setDailyPenaltyApplied(true);
            save();
        }
    }

    private boolean isDailyDone(LocalDate date, String dailyId){
        var log = getState().getAnna().getDaily().get(date.toString());
        if (dailyId == null) return false;
        switch (dailyId){
            case "nutrition": return log != null && log.isNutritionDailyAwarded();
            case "english": return log != null && log.isEnglishDailyAwarded();
            case "sport": return log != null && log.isSportAwarded();
            case "yoga": return log != null && log.isYogaAwarded();
            case "viet": return log != null && log.isVietWordsAwarded();
            default:
                if (dailyId.startsWith("g:")){
                    var set = getState().getAnna().getGenericDoneByDay()
                            .getOrDefault(date.toString(), Collections.emptySet());
                    return set.contains(dailyId.substring(2));
                }
                return false;
        }
    }

    synchronized void addBalance(int delta){
        var u = getState().getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta)); // balance never drops below 0
        log.info("Balance adjusted by {} to {}", delta, u.getBalance());
    }

    void addDailyWithRouletteBonus(String dailyId, int base){
        int mult = isRouletteDailyToday(dailyId) ? 2 : 1;
        addBalance(base * mult);
        if (mult == 2) {
            addHistory(LocalDate.now(zone()), "Рулетка бонус: " + prettyDaily(dailyId), base);
        }
    }

    private boolean isRouletteDailyToday(String dailyId){
        var u = getState().getAnna();
        var rs = u.getTodayRoulette();
        var today = LocalDate.now(zone());
        return rs != null
                && today.equals(rs.getDate())
                && rs.getEffect() == RouletteEffect.DAILY_X2
                && dailyId.equals(rs.getDailyId());
    }

    private int effectiveCostToday(String itemId, int baseCost){
        var rs = getState().getAnna().getTodayRoulette();
        if (rs == null || !LocalDate.now(zone()).equals(rs.getDate())) return baseCost;
        if (itemId.equals(rs.getFreeShopId())) return 0;
        if (itemId.equals(rs.getDiscountedShopId())) return Math.max(0, baseCost / 2);
        return baseCost;
    }

    void addHistory(LocalDate date, String label, int points){
        var extras = getState().getAnna().getHistoryExtras();
        extras.computeIfAbsent(date.toString(), k -> new ArrayList<>())
                .add(new HistoryDTO.Item(label, points));
    }

    private String prettyDaily(String id){
        if (id == null) return "";
        return switch (id) {
            case "nutrition" -> "Нутрициология";
            case "english" -> "Английский";
            case "sport" -> "Спорт";
            case "yoga" -> "Йога";
            case "viet" -> "Вьетнамские слова";
            default -> {
                if (id.startsWith("g:")) {
                    var gid = id.substring(2);
                    var opt = getState().getGenericDaily().stream()
                            .filter(g -> g.id().equals(gid))
                            .findFirst();
                    yield opt.map(GenericDailyTaskDef::title).orElse(gid);
                }
                yield id;
            }
        };
    }

    // --- Public API used by controllers ---
    public synchronized Map<String,Object> status() throws IOException {
        processDayBoundariesIfNeeded();
        var u = getState().getAnna();
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
        for (var def : getState().getGenericDaily()){
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
        map.put("goals", getState().getGoals());
        map.put("shop", getState().getShop());
        map.put("genericDaily", getState().getGenericDaily());
        map.put("todayGenericDone", todayGenericDone);
        map.put("genericStreaks", genericStreaks);
        return map;
    }


    public synchronized void resetStreaksIfMissedYesterday(){
        // Streaks reset when a day is missed. Minimal placeholder implementation.
    }

    public synchronized boolean completeGoal(String id) throws IOException {
        processDayBoundariesIfNeeded();
        for (int i = 0; i < getState().getGoals().size(); i++) {
            var g = getState().getGoals().get(i);
            if (g.id().equals(id)) {
                if (!g.isCompleted()) {
                    var updated = new OneTimeGoal(g.id(), g.title(), g.reward(), LocalDateTime.now(zone()));
                    getState().getGoals().set(i, updated);
                    int reward = g.reward();
                    var rs = getState().getAnna().getTodayRoulette();
                    if (rs != null
                            && LocalDate.now(zone()).equals(rs.getDate())
                            && rs.getEffect() == RouletteEffect.GOAL_X2
                            && g.id().equals(rs.getGoalId())) {
                        reward *= 2;
                        addHistory(LocalDate.now(zone()), "Рулетка бонус: цель x2 " + g.title(), g.reward());
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
        var u = getState().getAnna();
        var opt = getState().getShop().stream().filter(s -> s.id().equals(id)).findFirst();
        if (opt.isEmpty()) return false;
        var item = opt.get();
        int cost = effectiveCostToday(item.id(), item.cost());
        if (u.getBalance() < cost) return false;
        u.setBalance(u.getBalance() - cost);
        var when = LocalDateTime.now(zone());
        u.getPurchases().add(new Purchase(item.id(), item.title(), cost, when));
        save();
        return true;
    }

    public synchronized List<Purchase> getPurchases() throws IOException {
        processDayBoundariesIfNeeded();
        var list = new ArrayList<>(getState().getAnna().getPurchases());
        list.sort(Comparator.comparing(Purchase::purchasedAt).reversed());
        return list;
    }

    // --- Admin ---
    public synchronized List<ShopItem> setShop(List<ShopItem> items) throws IOException {
        getState().setShop(new ArrayList<>(items));
        save();
        return getState().getShop();
    }
    public synchronized List<OneTimeGoal> setGoals(List<OneTimeGoal> items) throws IOException {
        getState().setGoals(new ArrayList<>(items));
        save();
        return getState().getGoals();
    }
    public synchronized List<GenericDailyTaskDef> setGenericDaily(List<GenericDailyTaskDef> items) throws IOException {
        getState().setGenericDaily(new ArrayList<>(items));
       save();
        return getState().getGenericDaily();
    }

    // ===== Admin: balance
    public synchronized int adminAddBalance(int delta) throws IOException {
        var u = getState().getAnna();
        u.setBalance(Math.max(0, u.getBalance() + delta));
        save();
        return u.getBalance();
    }
    public synchronized int adminSetBalance(int value) throws IOException {
        var u = getState().getAnna();
        u.setBalance(Math.max(0, value));
        save();
        return u.getBalance();
    }
}
