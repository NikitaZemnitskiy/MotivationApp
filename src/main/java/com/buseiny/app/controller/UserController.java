package com.buseiny.app.controller;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{username}")
    public String getUserPage(@PathVariable String username, Model model) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return "error"; // если пользователя нет, показываем error.html
        }

        User user = optionalUser.get();

        List<DailyTask> sortedTasks = user.getDailyTasks().stream()
                .sorted(Comparator
                        .comparing((DailyTask t) -> t.getMinutesGoal() > 0 ? 0 : (t.getCountGoal() > 0 ? 1 : 2))
                        .thenComparing(DailyTask::getId))
                .toList();
        user.setDailyTasks(sortedTasks);
        model.addAttribute("user", user);
        System.out.println(user);
        return "user"; // user.html в templates
    }
}
