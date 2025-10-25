package com.buseiny.app.service;

import com.buseiny.app.model.User;
import com.buseiny.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUser(String username){
        return userRepository.findByUsername(username).get();
    }
}
