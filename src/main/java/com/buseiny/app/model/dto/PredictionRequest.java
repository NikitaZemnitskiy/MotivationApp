package com.buseiny.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionRequest {
    private String direction; // "up" или "down"
    private int amount;
}
