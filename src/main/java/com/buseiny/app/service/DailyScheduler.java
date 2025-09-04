package com.buseiny.app.service;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic maintenance tasks like streak resets and roulette penalties.
 */
@Component
@Slf4j
public class DailyScheduler {
    private final StateService state;

    public DailyScheduler(StateService state) {
        this.state = state;
    }
    //Every hour
    @Scheduled(cron = "0 0 * * * *", zone = "${app.timezone}")
    public void dailyMaintenance() {
        try {
            state.resetStreaksIfMissedYesterday();
        } catch (IOException e) {
            log.error("Failed to run daily maintenance", e);
        }
    }
}
