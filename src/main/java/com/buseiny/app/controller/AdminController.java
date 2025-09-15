package com.buseiny.app.controller;

import com.buseiny.app.model.DailyTaskDef;
import com.buseiny.app.model.OneTimeGoal;
import com.buseiny.app.model.ShopItem;
import com.buseiny.app.service.StateService;
import com.buseiny.app.service.HistoryService;
import com.buseiny.app.dto.AdminDayUpsertNewRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StateService state;
    private final HistoryService history;
    public AdminController(StateService state, HistoryService history){ this.state = state; this.history = history; }

    @PostMapping("/shop")
    public ResponseEntity<?> setShop(@RequestBody List<ShopItem> items) throws IOException {
        return ResponseEntity.ok(state.setShop(items));
    }

    @PostMapping("/goals")
    public ResponseEntity<?> setGoals(@RequestBody List<OneTimeGoal> items) throws IOException {
        return ResponseEntity.ok(state.setGoals(items));
    }

    @PostMapping("/daily")
    public ResponseEntity<?> setDailyTasks(@RequestBody List<DailyTaskDef> items) throws IOException {
        return ResponseEntity.ok(state.setDailyTasks(items));
    }

    // History endpoints reuse common calculations
    @GetMapping("/history/month")
    public ResponseEntity<?> month(@RequestParam("year") int year, @RequestParam("month") int month) throws Exception {
        return ResponseEntity.ok(history.computeMonthHistory(year, month));
    }

    @GetMapping("/history/day")
    public ResponseEntity<?> day(@RequestParam("date") String date) throws Exception {
        return ResponseEntity.ok(history.computeDayHistory(date));
    }

    // Adjust balance
    @PostMapping("/balance/add")
    public ResponseEntity<?> addBalance(@RequestParam("delta") int delta) throws Exception {
        int v = state.adminAddBalance(delta);
        return ResponseEntity.ok(Map.of("balance", v));
    }

    @PostMapping("/balance/set")
    public ResponseEntity<?> setBalance(@RequestParam("value") int value) throws Exception {
        int v = state.adminSetBalance(value);
        return ResponseEntity.ok(Map.of("balance", v));
    }

    // Edit day and recalculate totals
    @PostMapping("/day/upsert")
    public ResponseEntity<?> upsertDay(@RequestBody AdminDayUpsertNewRequest req) throws Exception {
        var result = history.adminUpsertDayAndRecalcNew(req);
        return ResponseEntity.ok(result);
    }

    // Gifts
    public record GiftRequest(String title, int amount) {}

    @PostMapping("/gifts/add")
    public ResponseEntity<?> addGift(@RequestBody GiftRequest req) throws IOException {
        return ResponseEntity.ok(state.adminAddGift(req.title(), req.amount()));
    }


}
