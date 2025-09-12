package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class BotService extends TelegramLongPollingBot { //TODO logs

    private final String botToken;

    public BotService(String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        long chatId = update.getMessage().getChatId();
        log.info("сообщение от: {},текст: {}", chatId ,update.getMessage().getText());
        // Обработка входящих сообщений
        if (update.hasMessage() && "/start".equals(update.getMessage().getText())) {
            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());
            response.setText("Добро пожаловать!");
            try {
                execute(response);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
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


}