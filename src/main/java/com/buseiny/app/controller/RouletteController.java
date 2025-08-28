package com.buseiny.app.controller;

import com.buseiny.app.dto.RouletteDTO;
import com.buseiny.app.service.StateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roulette")
public class RouletteController {
    private final StateService state;
    public RouletteController(StateService state){ this.state = state; }

    @GetMapping("/today")
    public ResponseEntity<RouletteDTO> today() throws Exception {
        return ResponseEntity.ok(state.getTodayRoulette());
    }

    @PostMapping("/spin")
    public ResponseEntity<RouletteDTO> spin() throws Exception {
        return ResponseEntity.ok(state.spinRoulette());
    }
}
