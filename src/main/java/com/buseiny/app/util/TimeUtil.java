package com.buseiny.app.util;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public class TimeUtil {
    public static ZoneId zone(String id) {
        return ZoneId.of(id);
    }
    public static LocalDate currentDate(ZoneId z){ return LocalDate.now(z); }
    public static LocalDate weekStartMonday(LocalDate d){
        // Move to Monday of current week
        DayOfWeek dow = d.getDayOfWeek();
        int shift = dow.getValue() - DayOfWeek.MONDAY.getValue();
        return d.minusDays(shift);
    }
    public static LocalDate weekEndSunday(LocalDate d){
        // Inclusive sunday date
        DayOfWeek dow = d.getDayOfWeek();
        int shift = DayOfWeek.SUNDAY.getValue() - dow.getValue();
        return d.plusDays(shift);
    }
    public static Instant weekEndInstant(LocalDate d, ZoneId z){
        LocalDate sunday = weekEndSunday(d);
        // End of sunday 23:59:59
        LocalDateTime end = sunday.atTime(23,59,59);
        return end.atZone(z).toInstant();
    }
    public static LocalDate firstMondayAfter(LocalDate d){
        LocalDate nextMon = d.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return nextMon;
    }
}
