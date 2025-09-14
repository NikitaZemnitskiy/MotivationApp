package com.buseiny.app.repository;

import com.buseiny.app.model.*;
import com.buseiny.app.model.DailyTaskDef;
import com.buseiny.app.model.DailyTaskKind;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.buseiny.app.util.TimeUtil;

@Repository
@Slf4j
public class StateRepository {

    @Value("${app.dataFile}")
    private String dataFile;

    private final ObjectMapper mapper;
    private AppState state;

    @Value("${app.timezone}")
    private String timezone;

    public StateRepository() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
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
        if (state.getGoals() == null || state.getGoals().isEmpty()) {
            seedGoals();
        }
        if (state.getShop() == null || state.getShop().isEmpty()) {
            seedShop();
        }
        if (state.getDailyTasks() == null || state.getDailyTasks().isEmpty()) {
            seedDailyTasksFromLegacy();
        }
        if (state.getAnna().getGifts() == null) {
            state.getAnna().setGifts(new ArrayList<>());
        }
        save();
    }

    private AppState freshState() {
        AppState s = new AppState();
        var zone = TimeUtil.zone(timezone);
        s.setInstalledAt(LocalDateTime.now(zone));
        s.setLastProcessedWeekStart(TimeUtil.weekStartMonday(LocalDate.now(zone)));
        UserState u = new UserState();
        u.setUsername("Anna");
        u.setBalance(0);
        u.setAvatarUrl("/assets/avatar.png");
        u.setGifts(new ArrayList<>());
        s.setAnna(u);
        return s;
    }

    private void seedGoals() {
        var goals = List.of(
                new OneTimeGoal("sunrise", "See the sunrise", 6),
                new OneTimeGoal("meet-vn-girl", "Meet a Vietnamese girl", 15),
                new OneTimeGoal("date-vn-girl", "Date a Vietnamese girl", 20)
        );
        state.setGoals(new ArrayList<>(goals));
    }

    private void seedShop() {
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

    private void seedDailyTasksFromLegacy() {
        List<DailyTaskDef> defs = new ArrayList<>();
        defs.add(DailyTaskDef.create(
                "nutrition", "Daily minutes #1", DailyTaskKind.MINUTES, 2,
                180, 900, false, 1
        ));
        defs.add(DailyTaskDef.create(
                "english", "Daily minutes #2", DailyTaskKind.MINUTES, 1,
                60, null, true, 1
        ));
        defs.add(DailyTaskDef.create(
                "sport", "Daily check #1", DailyTaskKind.CHECK, 1,
                null, null, true, 1
        ));
        defs.add(DailyTaskDef.create(
                "yoga", "Daily check #2", DailyTaskKind.CHECK, 1,
                null, null, false, 1
        ));
        defs.add(DailyTaskDef.create(
                "viet", "Daily check #3", DailyTaskKind.CHECK, 1,
                null, null, true, 1
        ));

        state.setDailyTasks(defs);
    }

    public synchronized AppState get() { return state; }

    public synchronized void save() throws IOException {
        mapper.writeValue(new File(dataFile), state);
        log.debug("State persisted to {}", dataFile);
    }
}

