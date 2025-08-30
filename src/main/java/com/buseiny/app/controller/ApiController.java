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

    // --- Daily tasks ---
    @PostMapping("/tasks/nutrition/add")
    public ResponseEntity<?> addNutrition(@RequestParam("minutes") int minutes) throws IOException {
        if (minutes != 15 && minutes != 30 && minutes != 60){
            return ResponseEntity.badRequest().body("minutes must be 15|30|60");
        }
        daily.addNutritionMinutes(minutes);
        return ResponseEntity.ok(state.status());
    }

    @PostMapping("/tasks/english/add")
    public ResponseEntity<?> addEnglish(@RequestParam("minutes") int minutes) throws IOException {
        if (minutes != 15 && minutes != 30 && minutes != 60){
            return ResponseEntity.badRequest().body("minutes must be 15|30|60");
        }
        daily.addEnglishMinutes(minutes);
        return ResponseEntity.ok(state.status());
    }

    @PostMapping("/tasks/sport/check")
    public ResponseEntity<?> checkSport() throws IOException {
        daily.checkSport();
        return ResponseEntity.ok(state.status());
    }

    @PostMapping("/tasks/yoga/check")
    public ResponseEntity<?> checkYoga() throws IOException {
        daily.checkYoga();
        return ResponseEntity.ok(state.status());
    }

    @PostMapping("/tasks/vietnamese/check")
    public ResponseEntity<?> checkViet() throws IOException {
        daily.checkVietWords();
        return ResponseEntity.ok(state.status());
    }

    // generic daily (admin-defined simple check task)
    @PostMapping("/tasks/generic/{id}/check")
    public ResponseEntity<?> checkGeneric(@PathVariable("id") String id) throws IOException {
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
}
