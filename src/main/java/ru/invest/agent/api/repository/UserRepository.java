package ru.invest.agent.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.invest.agent.api.constant.UserStatus;
import ru.invest.agent.api.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String>{
    Optional<User> findByPhone(String phone);

    List<User> findByUserStatus(UserStatus userStatus);

    Optional<User> findByTelegramChatId(String telegramChatId);
}
