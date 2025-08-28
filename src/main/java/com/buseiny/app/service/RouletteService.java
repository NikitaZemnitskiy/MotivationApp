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
    private static final List<FixedDaily> FIXED = List.of(
            new FixedDaily("nutrition", 2, d -> d != null && d.isNutritionDailyAwarded()),
            new FixedDaily("english", 1, d -> d != null && d.isEnglishDailyAwarded()),
            new FixedDaily("sport", 1, d -> d != null && d.isSportAwarded()),
            new FixedDaily("yoga", 1, d -> d != null && d.isYogaAwarded()),
            new FixedDaily("viet", 1, d -> d != null && d.isVietWordsAwarded())
    );

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
                for (var f : FIXED) {
                    candid.add(f.id());
                    rewardById.put(f.id(), f.reward());
                }
                for (var gd : state.getState().getGenericDaily()) {
                    candid.add("g:" + gd.getId());
                    rewardById.put("g:" + gd.getId(), gd.getDailyReward());
                }
                String pick = candid.get(ThreadLocalRandom.current().nextInt(candid.size()));
                rs.setDailyId(pick);
                rs.setDailyBaseReward(rewardById.get(pick));
            }
            case GOAL_X2 -> {
                var incomplete = state.getState().getGoals().stream().filter(g -> g.getCompletedAt() == null).toList();
                if (!incomplete.isEmpty()) {
                    var g = incomplete.get(ThreadLocalRandom.current().nextInt(incomplete.size()));
                    rs.setGoalId(g.getId());
                } else {
                    rs.setEffect(RouletteEffect.BONUS_POINTS);
                    rs.setBonusPoints(1 + ThreadLocalRandom.current().nextInt(5));
                    state.addBalance(rs.getBonusPoints());
                }
            }
            case BONUS_POINTS -> {
                int pts = 1 + ThreadLocalRandom.current().nextInt(5);
                rs.setBonusPoints(pts);
                state.addBalance(pts);
            }
            case SHOP_DISCOUNT_50 -> {
                var items = state.getState().getShop();
                if (!items.isEmpty()) {
                    var it = items.get(ThreadLocalRandom.current().nextInt(items.size()));
                    rs.setDiscountedShopId(it.id());
                }
            }
            case SHOP_FREE_UNDER_100 -> {
                var items = state.getState().getShop().stream().filter(i -> i.cost() < 100).toList();
                if (!items.isEmpty()) {
                    var it = items.get(ThreadLocalRandom.current().nextInt(items.size()));
                    rs.setFreeShopId(it.id());
                } else {
                    var all = state.getState().getShop();
                    if (!all.isEmpty()) {
                        var it = all.get(ThreadLocalRandom.current().nextInt(all.size()));
                        rs.setDiscountedShopId(it.id());
                        rs.setEffect(RouletteEffect.SHOP_DISCOUNT_50);
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
        return switch (id) {
            case "nutrition" -> "Nutrition";
            case "english" -> "English";
            case "sport" -> "Sport";
            case "yoga" -> "Yoga";
            case "viet" -> "5 Viet words";
            default -> {
                if (id.startsWith("g:")) yield genericTitle(id.substring(2));
                yield id;
            }
        };
    }

    private String goalTitle(String goalId) {
        if (goalId == null) return "";
        return state.getState().getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst().map(OneTimeGoal::getTitle).orElse(goalId);
    }

    private String shopTitle(String id) {
        if (id == null) return "";
        return state.getState().getShop().stream()
                .filter(s -> s.id().equals(id))
                .findFirst().map(ShopItem::title).orElse(id);
    }

    private String genericTitle(String gid) {
        return state.getState().getGenericDaily().stream()
                .filter(g -> g.getId().equals(gid))
                .findFirst().map(GenericDailyTaskDef::getTitle).orElse(gid);
    }
}
