package com.buseiny.app.controller;

import com.buseiny.app.model.History;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.UserRepository;
import com.buseiny.app.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequiredArgsConstructor
@RequestMapping("/roulette")
public class RouletteController {

    private final HistoryService historyService;
    private final UserRepository userRepository;

    @GetMapping("/{username}")
    public String userHistory(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username).get();
        model.addAttribute("user", user);
        return "roulette";
    }
}

