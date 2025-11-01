package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.api.model.User;


import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@Profile("!test")
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

        // Обработка callback-кнопок
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        log.debug("сообщение от: '{}',текст: '{}'", chatId ,messageText);

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

    public void sendNotification(String chatId, ReplyKeyboardMarkup replyKeyboardMarkup, String message) {
        SendMessage msg = new SendMessage(chatId, message);
        if (Objects.nonNull(replyKeyboardMarkup)) {
            msg.setReplyMarkup(replyKeyboardMarkup);
        }
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendNotification(String chatId, String message) {
        sendNotification(chatId,null, message);
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
        User existingUser = userService.findByPhone(phone);
        if (Objects.nonNull(existingUser)) {
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
            log.error("Ошибка отправки сообщения в чат '{}'", chatId, e);
        }
    }

    public InlineKeyboardMarkup createInlineKeyboardMarkup(){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        // Первый ряд кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        // Кнопка с callback данными
        row1.add(InlineKeyboardButton.builder()
                .text("Портфель 📊")
                .callbackData("PORTFOLIO_") // + chatId)
                .build());

        // Вторая кнопка в том же ряду
        row1.add(InlineKeyboardButton.builder()
                .text("Аналитика 📈")
                .callbackData("ANALYTICS")
                .build());

        // Второй ряд кнопок
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        // Кнопка с URL
        row2.add(InlineKeyboardButton.builder()
                .text("Открыть сайт 🌐")
                .url("https://invest.ru")
                .build());

//        // Кнопка с веб-приложением
//        row2.add(InlineKeyboardButton.builder()
//                .text("Торговля 💹")
//                .webApp(new WebAppInfo("https://trading.invest.ru"))
//                .build());

        // Третий ряд (одна кнопка)
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("Настройки ⚙️")
                .callbackData("SETTINGS")
                .build());

        // Собираем все ряды
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("Callback от {}: {}", chatId, callbackData);

        try {
            if (callbackData.startsWith("PORTFOLIO_")) {
                // Обработка запроса портфеля
                String response = "Информация о вашем портфеле...";
                editMessageText(chatId, messageId, response);
            } else if (callbackData.equals("ANALYTICS")) {
                // Обработка аналитики
                String response = "Аналитические данные...";
                sendTextMessage(chatId, response);
            }

            // Подтверждение обработки callback
            answerCallbackQuery(callbackQuery.getId());

        } catch (TelegramApiException e) {
            log.error("Ошибка обработки callback", e);
        }
    }

    private void editMessageText(long chatId, int messageId, String newText) throws TelegramApiException {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        execute(editMessage);
    }

    private void answerCallbackQuery(String callbackId) throws TelegramApiException {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        execute(answer);
    }

}