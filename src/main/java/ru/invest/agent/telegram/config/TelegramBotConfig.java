package ru.invest.agent.telegram.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.invest.agent.telegram.service.BotService;

@Slf4j
@Configuration
public class TelegramBotConfig {

    @Value("${telegram.chat.error_id}")
    private String chatId;
    @Value("${telegram.bot.token}")
    private String token;
    @Value("${telegram.bot.start.event.message}")
    private boolean isEventMessage;
//    @Value("${telegram.bot.user_name}")
//    private String userName;

    @Bean
    public BotService botService() {
        return new BotService(token);
    }

    @Bean
    public ApplicationRunner botInitializer(BotService botService) {
        return args -> {

            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(botService);
                log.info("Telegram бот успешно зарегистрирован");

                if (isEventMessage) {
                    botService.sendNotification(chatId, "🤖 Бот инициализирован и готов к работе!");
                }
            } catch (TelegramApiException e) {
                log.error("Ошибка регистрации Telegram бота: {}", e.getMessage());
            }
        };
    }

}