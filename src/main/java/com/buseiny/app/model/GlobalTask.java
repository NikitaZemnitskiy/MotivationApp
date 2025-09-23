package com.buseiny.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "global_tasks")
@Data
@ToString(exclude = {"user"})
public class GlobalTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private int reward = 0;
    private boolean completed = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
