package com.buseiny.app.dto;

import java.util.List;

public class HistoryDTO {
    public static class Item {
        public String label;
        public int points;
        public Item() {}
        public Item(String label, int points){ this.label = label; this.points = points; }
    }

    public static class DayHistory {
        public String date;        // yyyy-MM-dd
        public int total;          // суммарно за день
        public List<Item> items;   // детализация
    }

    public static class MonthHistory {
        public int year;
        public int month;          // 1..12
        public List<DayHistory> days; // только дни запрошенного месяца
    }
}
