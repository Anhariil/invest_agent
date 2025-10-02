package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.api.model.User;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BotService extends TelegramLongPollingBot { //TODO logs

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final Map<Long, String> USER_CREATION_STATES = new HashMap<>();

    private final String botToken;

    @Autowired
    private UserService userService;

    public BotService(String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        log.info("сообщение от: {},текст: {}", chatId ,messageText);

        // Проверка состояния создания пользователя
        if (USER_CREATION_STATES.containsKey(chatId)) {
            handlePhoneInput(chatId, messageText);
            return;
        }

        // Обработка команд
        switch (messageText) {
            case "/ping":
                sendTextMessage(chatId, "pong");
                break;
            case "/start":
                sendTextMessage(chatId, "Добро пожаловать!");
                break;
            case "/create":
                startUserCreation(chatId);
                break;
            default:
                sendTextMessage(chatId, "Неизвестная команда. Используйте /create для создания пользователя.");
        }
    }

    public void sendNotification(String chatId, String message) {
        SendMessage msg = new SendMessage(chatId, message);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "some_name"; //TODO где используется?
    }

    private void startUserCreation(long chatId) {
        USER_CREATION_STATES.put(chatId, "AWAITING_PHONE");
        sendTextMessage(chatId, "Введите ваш номер телефона в формате +7XXXXXXXXXX:");
    }

    private void handlePhoneInput(long chatId, String phone) {
        // Валидация номера телефона
        if (!isValidPhone(phone)) {
            sendTextMessage(chatId, "❌ Неверный формат телефона. Пожалуйста, введите номер в формате +7XXXXXXXXXX.");
            return;
        }

        // Проверка существования пользователя
        Optional<User> existingUser = userService.findByPhone(phone);
        if (existingUser.isPresent()) {
            sendTextMessage(chatId, "ℹ️ Пользователь с телефоном " + phone + " уже существует.");
        } else {
            // Создание нового пользователя
            User newUser = userService.createUser(phone,chatId);
            sendTextMessage(chatId, "✅ Пользователь успешно создан! ID: " + newUser.getId());
        }

        // Сброс состояния
        USER_CREATION_STATES.remove(chatId);
    }

    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в чат {}", chatId, e);
        }
    }

}