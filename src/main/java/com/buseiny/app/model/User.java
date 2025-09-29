package com.buseiny.app.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;
    private String displayName;
    private int balance = 0;
    private LocalDate lastWeaklyClean;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DailyTask> dailyTasks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GlobalTask> globalTasks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShopItem> shopItems = new ArrayList<>();

}
