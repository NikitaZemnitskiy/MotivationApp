package com.buseiny.app.controller;

import com.buseiny.app.model.GenericDailyTaskDef;
import com.buseiny.app.model.OneTimeGoal;
import com.buseiny.app.model.ShopItem;
import com.buseiny.app.service.StateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StateService state;
    public AdminController(StateService state){ this.state = state; }

    @PostMapping("/shop")
    public ResponseEntity<?> setShop(@RequestBody List<ShopItem> items) throws IOException {
        return ResponseEntity.ok(state.setShop(items));
    }

    @PostMapping("/goals")
    public ResponseEntity<?> setGoals(@RequestBody List<OneTimeGoal> items) throws IOException {
        return ResponseEntity.ok(state.setGoals(items));
    }

    @PostMapping("/daily/generic")
    public ResponseEntity<?> setGenericDaily(@RequestBody List<GenericDailyTaskDef> items) throws IOException {
        return ResponseEntity.ok(state.setGenericDaily(items));
    }

}
