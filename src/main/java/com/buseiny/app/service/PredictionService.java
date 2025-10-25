package com.buseiny.app.service;
import com.buseiny.app.model.HistoryType;
import com.buseiny.app.model.PredictionEntity;
import com.buseiny.app.model.User;
import com.buseiny.app.model.dto.PredictionRequest;
import com.buseiny.app.model.dto.PredictionResult;
import com.buseiny.app.repository.PredictionRepository;
import com.buseiny.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final HistoryService historyService;
    private final UserRepository userRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public double getBTCPrice() {
        try {
            JSONObject json = new JSONObject(new String(
                    new URL("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT").openStream().readAllBytes()));
            return json.getDouble("price");
        } catch (IOException e) {
            throw new RuntimeException("Error fetching BTC price", e);
        }
    }

    public void placePrediction(String username, PredictionRequest req) {

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        if (now.getHour() > 15) {
            return;
        }

        User user = userRepository.findByUsername(username).get();
        double currentPrice = getBTCPrice();
        PredictionEntity entity = new PredictionEntity();

        entity.setDirection(req.getDirection());
        entity.setAmount(req.getAmount());
        entity.setStartPrice(currentPrice);
        entity.setCreatedAt(LocalDateTime.now(VIETNAM_ZONE));

        user.setPredictionEntity(entity);
        entity.setUser(user);
        historyService.addHistory(user,-req.getAmount(), "Оплата предсказания", false, HistoryType.PREDICTION_PAY);
        user.setBalance(user.getBalance() - req.getAmount());
        userRepository.save(user);
    }

    public PredictionResult checkResult(String username) {
        User user = userRepository.findByUsername(username).get();
        PredictionEntity prediction = user.getPredictionEntity();
        if (prediction == null) {
            return new PredictionResult("Нет активного предсказания", false, 0, 0);
        }

        if(prediction.getCreatedAt().getDayOfYear() != LocalDateTime.now().getDayOfYear()){
            user.setPredictionEntity(null);
            userRepository.save(user);
            return new PredictionResult("Слишком поздно притопала забирать свой приз", false, 0, 0);
        }

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        if (now.getHour() < 22) {
            return new PredictionResult("Слишком рано проверять", false, 0, 0);
        }

        double endPrice = getBTCPrice();
        boolean won = (prediction.getDirection().equals("up") && endPrice > prediction.getStartPrice())
                || (prediction.getDirection().equals("down") && endPrice < prediction.getStartPrice());
        int reward = won ? prediction.getAmount() * 2 : 0;
        if(won){
            user.setBalance(user.getBalance() + reward);
            historyService.addHistory(user,reward, "Выигрыш предсказания", false, HistoryType.PREDICTION_WIN);
        }
        user.setPredictionEntity(null);
        userRepository.save(user);

        return new PredictionResult(
                won ? "Поздравляем! Ты угадала 💫" : "Увы, предсказание не сбылось 😢",
                won,
                reward,
                endPrice
        );
    }

    public PredictionEntity getCurrentPrediction(String username){
       return predictionRepository.findTopByUser_UsernameAndResolvedFalseOrderByCreatedAtDesc(username);
    }
}

