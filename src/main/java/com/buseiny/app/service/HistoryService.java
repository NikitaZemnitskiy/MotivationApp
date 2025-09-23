package com.buseiny.app.service;

import com.buseiny.app.model.History;
import com.buseiny.app.model.User;
import com.buseiny.app.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final HistoryRepository historyRepository;

    public History addHistory(User user, int amount, String reason, boolean isDaily) {
        History history = new History();
        history.setUser(user);
        history.setAmount(amount);
        history.setReason(reason);
        history.setDaily(isDaily);
        history.setTimestamp(LocalDateTime.now());
        return historyRepository.save(history);
    }

    public List<History> getUserHistory(User user) {
        return historyRepository.findByUserOrderByTimestampDesc(user);
    }
}
