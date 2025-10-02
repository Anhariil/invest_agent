package ru.invest.agent.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.invest.agent.api.constant.UserStatus;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setCreateDateTime(Instant.now());
        return userRepository.save(user);
    }

    public User createUser(String phone, long chatId) {
        User user = new User();
        user.setPhone(phone);
        user.setTelegramChatId(chatId);
        user.setCreateDateTime(Instant.now());
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllActive() {
        return userRepository.findByUserStatus(UserStatus.ACTIVE);
    }
}
