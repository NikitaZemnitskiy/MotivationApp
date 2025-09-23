package com.buseiny.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "tasks")
@Data
@ToString(exclude = {"user"})
public class DailyTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private int dailyReward = 0;
    private int dailyPenalty = 0;
    private int currentDoneToday = 0;

    //Type
    private boolean checked = false;
    private int minutesGoal = 0;
    private int countGoal = 0;

    //Streak
    private boolean isStreakEnabled = false;
    private int streakMultiplied = 2;
    private LocalDate lastCompletedDate;
    private int currentStreak;

    //Weakly
    private boolean hasWeeklyNorm = true;
    private int weeklyNorm = 0;
    private int currentDoneOnThisWeek = 0;
    private int weaklyReward = 1;
    private int weaklyPenalty = 0;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public DailyTask(){}
}
