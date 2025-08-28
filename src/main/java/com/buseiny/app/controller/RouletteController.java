package com.buseiny.app.controller;

import com.buseiny.app.dto.RouletteDTO;
import com.buseiny.app.service.RouletteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roulette")
public class RouletteController {
    private final RouletteService roulette;
    public RouletteController(RouletteService roulette){ this.roulette = roulette; }

    @GetMapping("/today")
    public ResponseEntity<RouletteDTO> today() throws Exception {
        return ResponseEntity.ok(roulette.getTodayRoulette());
    }

    @PostMapping("/spin")
    public ResponseEntity<RouletteDTO> spin() throws Exception {
        return ResponseEntity.ok(roulette.spinRoulette());
    }
}
