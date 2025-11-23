package ru.invest.agent.telegram.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.invest.agent.telegram.service.BotService;

import java.util.ArrayList;
import java.util.List;

import static ru.invest.agent.telegram.constant.BotCommand.*;

@Slf4j
@Configuration
@Profile("!test")
public class TelegramBotConfig {

    @Value("${telegram.chat.error_id}")
    private String chatId;
    @Value("${telegram.bot.token}")
    private String token;
    @Value("${telegram.bot.start.event.message}")
    private boolean isEventMessage;
    @Value("${telegram.bot.description}")
    private String botDescription;
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
                setCommands(botService);
                setDescription(botService);
                if (isEventMessage) {
//                    ReplyKeyboardMarkup keyboardMarkup = createReplyKeyboardMarkup(botService);
//                    botService.sendNotification(chatId, keyboardMarkup, "🤖 Бот инициализирован и готов к работе!");
                    botService.sendNotification(chatId, "🤖 Бот инициализирован и готов к работе!");
                }
                log.info("Telegram бот успешно зарегистрирован");
            } catch (TelegramApiException e) {
                log.error("Ошибка регистрации Telegram бота: {}", e.getMessage());
            }
        };
    }

    private void setDescription(BotService botService) throws TelegramApiException {
        SetMyDescription setDescription = new SetMyDescription();
        setDescription.setDescription(botDescription);
        botService.execute(setDescription);
    }

    private void setCommands(BotService botService) throws TelegramApiException {
        List<BotCommand> commands = new ArrayList<>();
        for (ru.invest.agent.telegram.constant.BotCommand command : ru.invest.agent.telegram.constant.BotCommand.values()){
            commands.add(new BotCommand(command.getCommand(),command.getDescription()));
        }

        SetMyCommands request = new SetMyCommands();
        request.setCommands(commands);
        request.setScope(new BotCommandScopeDefault());
        request.setLanguageCode("ru");
        botService.execute(request);
    }

    private ReplyKeyboardMarkup createReplyKeyboardMarkup(BotService botService) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Аналитика");
        row1.add("Портфель");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Настройки");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setKeyboard(List.of(row1, row2));
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        keyboard.setInputFieldPlaceholder("Выберите действие...");

        return keyboard;
    }


}