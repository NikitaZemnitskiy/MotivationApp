package com.buseiny.app.controller;

import com.buseiny.app.dto.HistoryDTO;
import com.buseiny.app.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService history;
    public HistoryController(HistoryService history){ this.history = history; }

    @GetMapping("/month")
    public ResponseEntity<HistoryDTO.MonthHistory> month(
            @RequestParam("year") int year,  @RequestParam("month") int month) throws IOException {
        return ResponseEntity.ok(history.computeMonthHistory(year, month));
    }

    @GetMapping("/day")
    public ResponseEntity<HistoryDTO.DayHistory> day( @RequestParam("date") String date) throws IOException {
        return ResponseEntity.ok(history.computeDayHistory(date));
    }
}
