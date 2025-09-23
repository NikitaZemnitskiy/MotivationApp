package com.buseiny.app.controller;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.service.TaskService;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/daily/complete/{taskId}/{count}")
    public String completeDaily(@PathVariable Long taskId, @PathVariable Integer count, @RequestParam String username) {
        taskService.completeDailyTask(taskId, count);
        return "redirect:/" + username;
    }

    @PostMapping("/global/complete/{taskId}")
    public String completeGlobal(@PathVariable Long taskId, @RequestParam String username) {
        taskService.completeGlobalTask(taskId);
        return "redirect:/" + username;
    }

    @PostMapping("/add")
    public String addTask(@ModelAttribute DailyTask task,
                          @RequestParam Long userId,
                          RedirectAttributes redirectAttributes) {
        try {
            taskService.addTask(task, userId);
            redirectAttributes.addFlashAttribute("success", "Задание успешно добавлено!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tasks/add";
        }
        return "redirect:/tasks/add";
    }

    @GetMapping("/add")
    public String showAddTaskForm(Model model) {
        model.addAttribute("task", new DailyTask());
        model.addAttribute("users", taskService.getAllUsers());
        return "add-task"; // имя html страницы
    }
}
