package com.buseiny.app.dto;

import java.util.Map;
import java.util.Set;

public record AdminDayUpsertNewRequest(
        String date,
        Map<String, Integer> minutes,
        Set<String> checks
) {}


