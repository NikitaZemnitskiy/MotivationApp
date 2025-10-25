package com.buseiny.app.controller;

import com.buseiny.app.model.User;
import com.buseiny.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PredictionController {
    private final UserService userService;

    @GetMapping("/prediction/{username}")
    public String predictionPage(@PathVariable String username, Model model) {
        model.addAttribute("user",userService.getUser(username));
        return "prediction";
    }
}
