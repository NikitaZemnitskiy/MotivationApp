package com.buseiny.app.dto;

import java.util.List;

/**
 * History of earned points grouped by day and month.
 */
public final class HistoryDTO {
    public record Item(String label, int points) {}
    public record DayHistory(String date, int total, List<Item> items) {}
    public record MonthHistory(int year, int month, List<DayHistory> days) {}
}
