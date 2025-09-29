package com.buseiny.app.controller;

import com.buseiny.app.model.ShopItem;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.UserRepository;
import com.buseiny.app.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ShopController {
    private final UserRepository userRepository;
    private final ShopService shopService;

    @GetMapping("/shop/{username}")
    public String shop(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ShopItem> items = user.getShopItems();

        model.addAttribute("user", user);
        model.addAttribute("items", items);
        return "shop";
    }

    @PostMapping("/shop/buy/{itemId}")
    public String buy(@PathVariable Long itemId, @RequestParam String username, RedirectAttributes redirectAttributes) {
        boolean success = shopService.buyItem(itemId, username);
        redirectAttributes.addAttribute("username", username);
        redirectAttributes.addAttribute("success", success);
        return success?"redirect:/shop/{username}?celebrate=true":"redirect:/shop/{username}";
    }

}

