package ru.invest.agent.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.constant.UserStatus;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.repository.UserRepository;
import ru.invest.agent.api.utils.InvestApiUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User findByPhone(String phone) {return userRepository.findByPhone(phone).orElse(null);}

    public User findByChatId(Long chatId) {return userRepository.findByTelegramChatId(chatId).orElse(null);}

    public User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setCreateDateTime(Instant.now());
        return save(user);
    }

    public User createUser(String phone, long chatId) {
        User user = new User();
        user.setPhone(phone);
        user.setTelegramChatId(chatId);
        user.setCreateDateTime(Instant.now());
        return save(user);
    }

    //TODO проверять на то что юзер уник и не перетирает данные
    public User createUser(User user) {
        //Пока возможно создание без ТФ
//        if (user.getPhone().isBlank()){
//            log.error("У пользователя отсутствует телефон");
//            throw new IllegalArgumentException("Отсутствует телефон");
//        }
        // для песочницы закидываем дефолтный токен и создаем новый аккаунт
        if (user.getTradeStatus() == TradeStatus.SANDBOX){
            user.setToken(InvestApiUtils.getSandboxToken());
            user.setMainAccount(InvestApiUtils.createTestAccount(user.getToken(), TradeStatus.SANDBOX));
        }

        user.setCreateDateTime(Instant.now());
        return save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllActive() {
        return userRepository.findByUserStatus(UserStatus.ACTIVE);
    }

    public User findById(String id) {return userRepository.findById(id).orElse(null);}

    public User updateUser(User user) {
        if ((!user.getId().isEmpty() && Objects.nonNull(findById(user.getId())))
            || (!user.getPhone().isEmpty() && Objects.nonNull(findByPhone(user.getPhone())))) {
            return save(user);
        }

        return createUser(user);
    }

    public User save(User user){
        return userRepository.save(user);
    }

    public String getSettings(User user) {
        return "Номер телефона: " + user.getPhone()
                + "\n Номер основного счета: " + user.getMainAccount().substring(0,user.getMainAccount().lastIndexOf("-"))+"***"
                + "\n Название основного счета: " + user.getMainAccountName()
                + "\n Количество привязанных счетов: " + user.getAccounts().size()
                + "\n Статус пользователя: " + user.getUserStatus()
                + "\n Статус торговли бота: " + user.getTradeStatus().getLabel();
    }
}
