package com.buseiny.app.controller;

import com.buseiny.app.model.PredictionEntity;
import com.buseiny.app.model.dto.PredictionRequest;
import com.buseiny.app.model.dto.PredictionResult;
import com.buseiny.app.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{username}/prediction")
@RequiredArgsConstructor
public class PredictionApiController {

    private final PredictionService predictionService;

    @GetMapping("/btc")
    public double getBTC() {
        return predictionService.getBTCPrice();
    }

    @PostMapping("/place")
    public ResponseEntity<String> placePrediction(
            @PathVariable String username,
            @RequestBody PredictionRequest request){
        predictionService.placePrediction(username, request);
        return ResponseEntity.ok("Ставка принята ✅");
    }

    @GetMapping("/result")
    public PredictionResult checkResult(@PathVariable String username) {
        return predictionService.checkResult(username);
    }

    @GetMapping("/current")
    public ResponseEntity<PredictionEntity> getCurrentPrediction(@PathVariable String username) {
        PredictionEntity current = predictionService.getCurrentPrediction(username);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(current);
    }
}

