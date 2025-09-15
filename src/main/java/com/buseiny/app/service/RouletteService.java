package com.buseiny.app.service;

import com.buseiny.app.dto.RouletteDTO;
import com.buseiny.app.model.*;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Service
@Slf4j
public class RouletteService {
    private final StateService state;
    private record FixedDaily(String id, int reward, Function<DailyLog, Boolean> isDone) {}

    public RouletteService(StateService state) {
        this.state = state;
    }

    public synchronized RouletteDTO getTodayRoulette() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        var today = LocalDate.now(state.zone());
        if (u.getTodayRoulette() != null && today.equals(u.getTodayRoulette().getDate())) {
            return toDTO(u.getTodayRoulette(), false, effectMessage(u.getTodayRoulette()));
        }
        var rs = new RouletteState();
        rs.setDate(today);
        return toDTO(rs, true, "Spin the roulette ✨");
    }

    public synchronized RouletteDTO spinRoulette() throws IOException {
        state.processDayBoundariesIfNeeded();
        var u = state.getState().getAnna();
        var today = LocalDate.now(state.zone());
        if (u.getTodayRoulette() != null && today.equals(u.getTodayRoulette().getDate())) {
            return toDTO(u.getTodayRoulette(), false, "Already spun today 💫");
        }
        int roll = ThreadLocalRandom.current().nextInt(100);
        RouletteEffect eff;
        if (roll < 40) eff = RouletteEffect.DAILY_X2;
        else if (roll < 70) eff = RouletteEffect.GOAL_X2;
        else if (roll < 80) eff = RouletteEffect.BONUS_POINTS;
        else if (roll < 90) eff = RouletteEffect.SHOP_DISCOUNT_50;
        else eff = RouletteEffect.SHOP_FREE_UNDER_100;

        var rs = new RouletteState();
        rs.setDate(today);
        rs.setEffect(eff);

        switch (eff) {
            case DAILY_X2 -> {
                List<String> candid = new ArrayList<>();
                Map<String, Integer> rewardById = new HashMap<>();
                for (var def : state.getState().getDailyTasks()){
                    candid.add(def.id());
                    rewardById.put(def.id(), def.dailyReward());
                }
                String pick = candid.get(ThreadLocalRandom.current().nextInt(candid.size()));
                rs.setDailyId(pick);
                rs.setDailyBaseReward(rewardById.get(pick));
                // record zero-point history entry to reflect roulette effect selection
                state.addHistory(today, "Рулетка эффект: DAILY_X2 — " + prettyDaily(pick), 0);
            }
            case GOAL_X2 -> {
                var incomplete = state.getState().getGoals().stream().filter(g -> g.completedAt() == null).toList();
                if (!incomplete.isEmpty()) {
                    var g = incomplete.get(ThreadLocalRandom.current().nextInt(incomplete.size()));
                    rs.setGoalId(g.id());
                    state.addHistory(today, "Рулетка эффект: GOAL_X2 — " + goalTitle(g.id()), 0);
                } else {
                    rs.setEffect(RouletteEffect.BONUS_POINTS);
                    rs.setBonusPoints(1 + ThreadLocalRandom.current().nextInt(5));
                    state.addBalance(rs.getBonusPoints());
                    state.addHistory(LocalDate.now(state.zone()), "Рулетка бонус", rs.getBonusPoints());
                }
            }
            case BONUS_POINTS -> {
                int pts = 1 + ThreadLocalRandom.current().nextInt(5);
                rs.setBonusPoints(pts);
                state.addBalance(pts);
                state.addHistory(LocalDate.now(state.zone()), "Рулетка бонус", pts);
            }
            case SHOP_DISCOUNT_50 -> {
                var items = state.getState().getShop();
                if (!items.isEmpty()) {
                    var it = items.get(ThreadLocalRandom.current().nextInt(items.size()));
                    rs.setDiscountedShopId(it.id());
                    state.addHistory(today, "Рулетка эффект: SHOP_DISCOUNT_50 — " + shopTitle(it.id()), 0);
                }
            }
            case SHOP_FREE_UNDER_100 -> {
                var items = state.getState().getShop().stream().filter(i -> i.cost() < 100).toList();
                if (!items.isEmpty()) {
                    var it = items.get(ThreadLocalRandom.current().nextInt(items.size()));
                    rs.setFreeShopId(it.id());
                    state.addHistory(today, "Рулетка эффект: SHOP_FREE_UNDER_100 — " + shopTitle(it.id()), 0);
                } else {
                    var all = state.getState().getShop();
                    if (!all.isEmpty()) {
                        var it = all.get(ThreadLocalRandom.current().nextInt(all.size()));
                        rs.setDiscountedShopId(it.id());
                        rs.setEffect(RouletteEffect.SHOP_DISCOUNT_50);
                        state.addHistory(today, "Рулетка эффект: SHOP_DISCOUNT_50 — " + shopTitle(it.id()), 0);
                    }
                }
            }
        }

        u.setTodayRoulette(rs);
        log.info("Roulette rolled: {}", rs.getEffect());
        state.save();
        return toDTO(rs, false, effectMessage(rs));
    }

    private RouletteDTO toDTO(RouletteState rs, boolean canSpin, String msg) {
        return new RouletteDTO(
                rs.getDate().toString(),
                rs.getEffect(),
                rs.getDailyId(),
                rs.getDailyBaseReward(),
                rs.getGoalId(),
                rs.getBonusPoints(),
                rs.getDiscountedShopId(),
                rs.getFreeShopId(),
                canSpin,
                msg,
                rs.getDate().plusDays(1).atStartOfDay(state.zone()).toInstant().toString()
        );
    }

    private String effectMessage(RouletteState rs) {
        if (rs.getEffect() == null) return "";
        return switch (rs.getEffect()) {
            case DAILY_X2 -> "Today's daily \"" + prettyDaily(rs.getDailyId()) + "\" gives x2, penalty for skipping (-" + rs.getDailyBaseReward() + ")!";
            case GOAL_X2 -> "Today's goal x2: " + goalTitle(rs.getGoalId());
            case BONUS_POINTS -> "Instant bonus: +" + rs.getBonusPoints();
            case SHOP_DISCOUNT_50 -> "-50% on purchase: " + shopTitle(rs.getDiscountedShopId());
            case SHOP_FREE_UNDER_100 -> "Free today: " + shopTitle(rs.getFreeShopId());
        };
    }

    private String prettyDaily(String id) {
        if (id == null) return "";
        return state.getState().getDailyTasks().stream()
                .filter(d -> d.id().equals(id))
                .findFirst().map(com.buseiny.app.model.DailyTaskDef::title)
                .orElse(id);
    }

    private String goalTitle(String goalId) {
        if (goalId == null) return "";
        return state.getState().getGoals().stream()
                .filter(g -> g.id().equals(goalId))
                .findFirst().map(OneTimeGoal::title).orElse(goalId);
    }

    private String shopTitle(String id) {
        if (id == null) return "";
        return state.getState().getShop().stream()
                .filter(s -> s.id().equals(id))
                .findFirst().map(ShopItem::title).orElse(id);
    }

    private String genericTitle(String gid) {
        return gid;
    }
}
