package com.buseiny.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionResult {
    private String message;
    private boolean won;
    private double reward;
    private double endPrice;

    public PredictionResult(String message, boolean won) {
        this.message = message;
        this.won = won;
    }
}
