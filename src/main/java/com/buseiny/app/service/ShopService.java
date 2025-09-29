package com.buseiny.app.service;

import com.buseiny.app.model.History;
import com.buseiny.app.model.HistoryType;
import com.buseiny.app.model.ShopItem;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.HistoryRepository;
import com.buseiny.app.repository.ShopItemRepository;
import com.buseiny.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;

    public List<ShopItem> getAllItems(String username) {
        return userRepository.findByUsername(username).get().getShopItems();
    }

    public boolean buyItem(Long itemId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        ShopItem item = user.getShopItems().stream().filter(a -> a.getId().equals(itemId)).findFirst().orElseThrow();

        if (user.getBalance() < item.getPrice()) {
            return false; // недостаточно бусёинов
        }

        user.setBalance(user.getBalance() - item.getPrice());
        userRepository.save(user);

        // записываем в историю
        History history = new History();
        history.setUser(user);
        history.setAmount(-item.getPrice());
        history.setReason("Куплен: " + item.getName());
        history.setTimestamp(LocalDateTime.now());
        history.setDate(LocalDate.now());
        history.setType(HistoryType.SHOP);
        historyRepository.save(history);

        return true;
    }
}

