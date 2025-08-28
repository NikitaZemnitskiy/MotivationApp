package com.buseiny.app.model;

import java.time.LocalDateTime;

public record Purchase(String shopItemId, String titleSnapshot, int costSnapshot, LocalDateTime purchasedAt) {}
