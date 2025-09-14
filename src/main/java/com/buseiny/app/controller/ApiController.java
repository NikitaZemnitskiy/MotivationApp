package com.buseiny.app.controller;

import com.buseiny.app.service.StateService;
import com.buseiny.app.service.DailyTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import com.buseiny.app.model.Purchase;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StateService state;
    private final DailyTaskService daily;

    public ApiController(StateService state, DailyTaskService daily){
        this.state = state;
        this.daily = daily;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() throws IOException {
        Map<String,Object> status = state.status();
        return ResponseEntity.ok(status);
    }

    // --- Daily tasks (legacy endpoints kept) ---
    // legacy endpoints removed

    // generic daily (unified id)
    @PostMapping("/tasks/generic/{id}/check")
    public ResponseEntity<?> checkGeneric(@PathVariable("id") String id) throws IOException {
        daily.checkGenericTask(id);
        return ResponseEntity.ok(state.status());
    }

    // Unified: add minutes to a minutes-type task
    @PostMapping("/tasks/{id}/add")
    public ResponseEntity<?> addMinutesGeneric(@PathVariable("id") String id, @RequestParam("minutes") int minutes) throws IOException {
        if (minutes <= 0) return ResponseEntity.badRequest().body("minutes must be > 0");
        daily.addMinutesByTaskId(id, minutes);
        return ResponseEntity.ok(state.status());
    }

    // Unified: check a check-type task
    @PostMapping("/tasks/{id}/check")
    public ResponseEntity<?> checkGenericUnified(@PathVariable("id") String id) throws IOException {
        daily.checkGenericTask(id);
        return ResponseEntity.ok(state.status());
    }

    // --- One-time goals ---
    @PostMapping("/goals/{id}/complete")
    public ResponseEntity<?> completeGoal(@PathVariable("id") String id) throws IOException {
        boolean ok = state.completeGoal(id);
        if (!ok) return ResponseEntity.badRequest().body("already completed or not found");
        return ResponseEntity.ok(state.status());
    }

    // --- Shop ---
    @PostMapping("/shop/{id}/purchase")
    public ResponseEntity<?> purchase(@PathVariable("id") String id) throws IOException {
        boolean ok = state.purchase(id);
        if (!ok) return ResponseEntity.badRequest().body("not enough balance or not found");
        return ResponseEntity.ok(state.status());
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<Purchase>> purchases() throws IOException {
        return ResponseEntity.ok(state.getPurchases());
    }

    // --- Gifts ---
    @PostMapping("/gifts/{id}/accept")
    public ResponseEntity<?> acceptGift(@PathVariable("id") String id) throws IOException {
        boolean ok = state.acceptGift(id);
        if (!ok) return ResponseEntity.badRequest().body("not found");
        return ResponseEntity.ok(state.status());
    }
}
