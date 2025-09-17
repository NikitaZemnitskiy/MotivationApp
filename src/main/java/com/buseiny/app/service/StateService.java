package com.buseiny.app.service;

import com.buseiny.app.dto.HistoryDTO;
import com.buseiny.app.model.*;
import com.buseiny.app.repository.StateRepository;
import com.buseiny.app.util.TimeUtil;
import com.buseiny.app.dto.WeeklyTaskDTO;
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

    private String todayKey(){
        return LocalDate.now(zone()).toString();
    }

    DailyLog todayLog(){
        var daily = getState().getAnna().getDaily();
        return daily.computeIfAbsent(todayKey(), k -> new DailyLog());
    }

    int sumWeeklyGoalMinutesForWeek(LocalDate weekStart){
        // Sums minutes for the first minutes-type task that has a weekly goal
        var minutesTaskOpt = getState().getDailyTasks().stream()
                .filter(t -> t.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES && t.weeklyMinutesGoal() != null && t.weeklyMinutesGoal() > 0)
                .findFirst();
        if (minutesTaskOpt.isEmpty()) return 0;
        String taskId = minutesTaskOpt.get().id();
        LocalDate d = weekStart;
        int sum = 0;
        for (int i=0;i<7;i++){
            String key = d.toString();
            var log = getState().getAnna().getDaily().get(key);
            if (log != null){
                Integer m = log.getMinutes().get(taskId);
                if (m != null) sum += m;
            }
            d = d.plusDays(1);
        }
        return sum;
    }

    private int countDailyForWeek(LocalDate weekStart, String dailyId){
        LocalDate d = weekStart;
        int cnt = 0;
        for (int i=0;i<7;i++){
            if (isDailyDone(d, dailyId)) cnt++;
            d = d.plusDays(1);
        }
        return cnt;
    }

    private int weeklyRequirement(String dailyId){
        var opt = getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(dailyId))
                .findFirst();
        return opt.map(d -> d.weeklyRequiredCount() == null || d.weeklyRequiredCount() <= 0 ? 1 : d.weeklyRequiredCount()).orElse(1);
    }

    private int dailyRewardById(String dailyId){
        return getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(dailyId))
                .findFirst()
                .map(com.buseiny.app.model.DailyTaskDef::dailyReward)
                .orElse(0);
    }

    private void applyWeeklyPenalties(LocalDate weekStart){
        var today = LocalDate.now(zone());
        List<String> ids = new ArrayList<>();
        for (var def : getState().getDailyTasks()) ids.add(def.id());
        for (var id : ids){
            int done = countDailyForWeek(weekStart, id);
            int req = weeklyRequirement(id);
            if (done < req){
                int miss = req - done;
                int pen = -5 * Math.abs(dailyRewardById(id)) * miss;
                if (pen != 0){
                    addBalance(pen);
                    addHistory(today, "Штраф за неделю: " + prettyDaily(id), pen);
                }
            }
        }
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
                var minutesTaskOpt = getState().getDailyTasks().stream()
                        .filter(t -> t.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES && t.weeklyMinutesGoal() != null && t.weeklyMinutesGoal() > 0)
                        .findFirst();
                if (minutesTaskOpt.isPresent()){
                    int minutes = sumWeeklyGoalMinutesForWeek(weekStart);
                    int goal = minutesTaskOpt.get().weeklyMinutesGoal();
                    if (minutes >= goal){
                        addBalance(14);
                    } else {
                        addBalance(-20);
                    }
                }
                applyWeeklyPenalties(weekStart);
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
            String id = rs.getDailyId();
            if (id == null) {
                rs.setDailyPenaltyApplied(true);
                save();
                return;
            }
            if (id.startsWith("g:")) {
                id = id.substring(2);
                rs.setDailyId(id);
            }
            Integer base = rs.getDailyBaseReward();
            if (base == null) {
                base = dailyRewardById(id);
                rs.setDailyBaseReward(base);
            }
            var dailyDone = isDailyDone(rs.getDate(), id);
            if (!dailyDone && base != null && base != 0) {
                int pen = -Math.abs(base);
                addBalance(pen);
                addHistory(today, "Штраф за пропуск: " + prettyDaily(id), pen);
            }
            rs.setDailyPenaltyApplied(true);
            save();
        }
    }

    private boolean isDailyDone(LocalDate date, String dailyId){
        if (dailyId == null) return false;
        var log = getState().getAnna().getDaily().get(date.toString());
        var opt = getState().getDailyTasks().stream().filter(d -> d.id().equals(dailyId)).findFirst();
        if (opt.isEmpty()) return false;
        var def = opt.get();
        if (log == null) return false;
        if (def.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES) {
            Integer m = log.getMinutes().get(dailyId);
            return m != null && def.minutesPerDay() != null && m >= def.minutesPerDay();
        } else {
            return log.getChecks().contains(dailyId);
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

    public synchronized void addBalanceWithHistory(LocalDate date, String label, int delta){
        addBalance(delta);
        addHistory(date, label, delta);
    }

    private String prettyDaily(String id){
        if (id == null) return "";
        return getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(id))
                .findFirst()
                .map(com.buseiny.app.model.DailyTaskDef::title)
                .orElse(id);
    }

    public synchronized Map<String,Object> status() throws IOException {
        processDayBoundariesIfNeeded();
        var u = getState().getAnna();
        var today = LocalDate.now(zone());
        var weekStart = TimeUtil.weekStartMonday(today);
        var weekEndInstant = TimeUtil.weekEndInstant(today, zone());
        int weekMinutes = 0;
        Integer weekGoalMinutes = null;
        var minutesTaskOpt = getState().getDailyTasks().stream()
                .filter(t -> t.kind() == com.buseiny.app.model.DailyTaskKind.MINUTES && t.weeklyMinutesGoal() != null && t.weeklyMinutesGoal() > 0)
                .findFirst();
        if (minutesTaskOpt.isPresent()){
            var minutesTask = minutesTaskOpt.get();
            weekGoalMinutes = minutesTask.weeklyMinutesGoal();
            for (int i=0;i<7;i++){
                var d = weekStart.plusDays(i).toString();
                var log = u.getDaily().get(d);
                if (log != null) weekMinutes += log.getMinutes().getOrDefault(minutesTask.id(), 0);
            }
        }

        var todayLog = u.getDaily().getOrDefault(today.toString(), new DailyLog());

        Map<String, Boolean> todayGenericDone = new HashMap<>();

        Map<String, Integer> genericStreaks = new HashMap<>(u.getStreaks());

        Map<String,Object> map = new HashMap<>();
        map.put("username", u.getUsername());
        map.put("avatarUrl", u.getAvatarUrl());
        map.put("balance", u.getBalance());
        map.put("weekMinutes", weekMinutes);
        map.put("weekGoalMinutes", weekGoalMinutes == null ? 0 : weekGoalMinutes);
        map.put("secondsUntilWeekEndEpoch", weekEndInstant.getEpochSecond());
        map.put("currentWeekStart", weekStart.toString());
        map.put("goals", getState().getGoals());
        // shop with effectiveCost (considering today's roulette discounts)
        List<Map<String,Object>> shopList = new ArrayList<>();
        for (var s : getState().getShop()){
            Map<String,Object> it = new HashMap<>();
            it.put("id", s.id());
            it.put("title", s.title());
            it.put("cost", s.cost());
            it.put("effectiveCost", effectiveCostToday(s.id(), s.cost()));
            shopList.add(it);
        }
        map.put("shop", shopList);
        // Unified tasks list with today's state
        // Unified tasks list with today's state
        List<Map<String,Object>> tasks = new ArrayList<>();
        for (var def : getState().getDailyTasks()){
            Map<String,Object> t = new HashMap<>();
            t.put("id", def.id());
            t.put("title", def.title());
            t.put("kind", def.kind().name());
            t.put("dailyReward", def.dailyReward());
            t.put("minutesPerDay", def.minutesPerDay());
            t.put("weeklyMinutesGoal", def.weeklyMinutesGoal());
            t.put("streakEnabled", def.streakEnabled());
            t.put("weeklyRequiredCount", def.weeklyRequiredCount());
            int todayMinutes = todayLog.getMinutes().getOrDefault(def.id(), 0);
            boolean todayDone = isDailyDone(today, def.id());
            int streak = 0;
            if (def.streakEnabled()){
                streak = u.getStreaks().getOrDefault(def.id(), 0);
            }
            t.put("todayMinutes", todayMinutes);
            t.put("todayDone", todayDone);
            t.put("streak", streak);
            tasks.add(t);
        }
        map.put("tasks", tasks);
        List<WeeklyTaskDTO> weekly = new ArrayList<>();
        for (var def : getState().getDailyTasks()){
            weekly.add(new WeeklyTaskDTO(def.id(), def.title(), weeklyRequirement(def.id()), countDailyForWeek(weekStart, def.id())));
        }
        map.put("weekDaily", weekly);
        map.put("gifts", u.getGifts());
        return map;
    }


    public synchronized void resetStreaksIfMissedYesterday() throws IOException {
        processDayBoundariesIfNeeded();
        var u = getState().getAnna();
        var zone = zone();
        var today = LocalDate.now(zone);
        var yesterday = today.minusDays(1);
        var yLog = u.getDaily().get(yesterday.toString());

        boolean changed = false;

        var streaks = u.getStreaks();
        for (var def : getState().getDailyTasks()) {
            if (!def.streakEnabled()) continue;
            boolean done = false;
            if (yLog != null) {
                if (def.kind() == DailyTaskKind.MINUTES) {
                    Integer m = yLog.getMinutes().get(def.id());
                    done = m != null && def.minutesPerDay() != null && m >= def.minutesPerDay();
                } else {
                    done = yLog.getChecks().contains(def.id());
                }
            }
            if (!done && streaks.getOrDefault(def.id(), 0) != 0) { streaks.put(def.id(), 0); changed = true; }
        }

        if (changed) {
            save();
            log.info("Streaks reset for missed tasks on {}", yesterday);
        }
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

    public synchronized boolean acceptGift(String id) throws IOException {
        processDayBoundariesIfNeeded();
        var u = getState().getAnna();
        var it = u.getGifts().iterator();
        while (it.hasNext()) {
            var g = it.next();
            if (g.id().equals(id)) {
                addBalanceWithHistory(LocalDate.now(zone()), "Подарок: " + g.title(), g.amount());
                it.remove();
                save();
                return true;
            }
        }
        return false;
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
    public synchronized List<DailyTaskDef> setDailyTasks(List<DailyTaskDef> items) throws IOException {
        getState().setDailyTasks(new ArrayList<>(items));
        save();
        return getState().getDailyTasks();
    }

    // ===== Admin: balance
    public synchronized int adminAddBalance(int delta) throws IOException {
        var u = getState().getAnna();
        addBalanceWithHistory(LocalDate.now(zone()), "Админ: корректировка баланса", delta);
        save();
        return u.getBalance();
    }
    public synchronized int adminSetBalance(int value) throws IOException {
        var u = getState().getAnna();
        int old = u.getBalance();
        int newValue = Math.max(0, value);
        u.setBalance(newValue);
        int delta = newValue - old;
        if (delta != 0){
            addHistory(LocalDate.now(zone()), "Админ: установка баланса", delta);
        }
        save();
        return u.getBalance();
    }

    public synchronized List<Gift> adminAddGift(String title, int amount) throws IOException {
        var u = getState().getAnna();
        u.getGifts().add(new Gift(UUID.randomUUID().toString(), title, amount));
        save();
        return u.getGifts();
    }
}
