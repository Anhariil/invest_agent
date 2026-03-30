package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.constant.UserStatus;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.telegram.constant.BotCommand;
import ru.invest.agent.telegram.constant.InlineButton;
import ru.invest.agent.telegram.exception.InvestServiceException;
import ru.invest.agent.telegram.utils.ChartGenerator;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

import static ru.invest.agent.telegram.constant.InlineButton.*;

@Slf4j
@Service
@Profile("!test")
public class BotService extends TelegramLongPollingBot { //TODO logs

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final Map<Long, String> USER_CREATION_STATES = new HashMap<>();
    private static final Map<Long, TradeStatus> USER_CHANGE_T_STATUS_STATES = new HashMap<>();

    private final String botToken;
    private final String cancelCreationText = "Отмена";
    private final String continueCreationText = "Продолжить без телефона";

    @Autowired
    private UserService userService;

    @Autowired
    private InvestService investService;

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

        // Обработка получения контакта
        if (update.hasMessage() && update.getMessage().hasContact()) {
            handleReceivedContact(update.getMessage());
            return;
        }

        // Если сообщение не содержит текста - выходим
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        // Обработка текстовых команд / полученного текста
        handleTextCommand(update);
    }

    private void handleTextCommand(Update update) {

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        log.debug("сообщение от: '{}',текст: '{}'", chatId, messageText);

        //Обработка текстовых команд
        if (cancelCreationText.equals(messageText) && USER_CREATION_STATES.containsKey(chatId)) {
            removeReplyKeyboard(chatId, "❌ Создание пользователя отменено");
            USER_CREATION_STATES.remove(chatId);
            return;
        } else if (continueCreationText.equals(messageText) && USER_CREATION_STATES.containsKey(chatId)) {
            String message = userCreation(update.getMessage());
            removeReplyKeyboard(chatId, message);
            USER_CREATION_STATES.remove(chatId);
            return;
        }

        //обработка входящего токена при смене статуса
        if (USER_CHANGE_T_STATUS_STATES.containsKey(chatId))
        {
            if(investService.isTokenValid(messageText,USER_CHANGE_T_STATUS_STATES.get(chatId))){
                User user = userService.findByChatId(chatId);
                user.setToken(messageText);
                user.setUserStatus(UserStatus.ACTIVE); //TODO надо ли обновлять статус на активный?
                user.setTradeStatus(USER_CHANGE_T_STATUS_STATES.get(chatId));
                userService.save(user);
                sendNotification(chatId,"Обновление статуса завершено успешно!\nНовый статус: " + user.getTradeStatus());
            } else {
                sendNotification(chatId,"❌ Обновление статуса завершено c ошибкой!\nПеред повторной сменой статуса, " +
                        "проверьте корректность введенного токена.");
            }
            USER_CHANGE_T_STATUS_STATES.remove(chatId);
            return;
        }

        if (BotCommand.contains(messageText)) {
            // Обработка команд
            switch (BotCommand.getByCommand(messageText)) {
                case PING:
                    sendTextMessage(chatId, "pong");
                    break;
                case START:
                    handleStartCommand(chatId);
                    break;
                case SANDBOX:
                    if (userService.findByChatId(chatId) == null) {
                        startSandboxCreation(chatId);
                    } else {
                        sendTextMessage(chatId, "ℹ️ Пользователь уже существует. Вы можете продолжить работать без " +
                                "создания аккаунта.");
                    }
                    break;
                case CREATE:
                    if (userService.findByChatId(chatId) == null) {
                        startUserCreation(chatId);
                    } else {
                        sendTextMessage(chatId, "ℹ️ Пользователь уже существует. Вы можете продолжить работать без " +
                                "создания аккаунта.");
                    }
                    break;
                case CHART_EXAMPLE:
                    sendBarChartExample(chatId);
                    break;
                case HELP: //TODO
                    sendTextMessage(chatId, "Тут будет описание команд. Однажды. Возможно.\n А пока используйте " +
                            BotCommand.START.getCommand() + " или " + BotCommand.CREATE.getCommand() +".");
            }
        } else {
            sendTextMessage(chatId, "Неизвестная команда. Используйте " + BotCommand.HELP.getCommand() +
                    " для получения информации по командам.");
        }
    }

    private void sendBarChartExample(long chatId) {
        try {
            // Генерируем данные для графика
            double[] values = {3, 7, 5};

//            // Создаем изображение
//            byte[] chartImage = ChartGenerator.generateSimpleBarChart("Пример графика", new String[]{"Актив А","Актив Б","Актив Е"}
//                    , values, "Наименование актива", "Значение актива");
            // Создаем изображение
            byte[] chartImage = ChartGenerator.generateSimplePieChart("Пример графика", new String[]{"Актив А", "Актив Б", "Актив Е"}
                    , values);

            // Создаем объект для отправки фото
            SendPhoto photo = new SendPhoto();
            photo.setChatId(String.valueOf(chatId));
            photo.setPhoto(new InputFile(new ByteArrayInputStream(chartImage), "chart.png"));
            photo.setCaption("График с значениями: 3, 7, 5");

            // Отправляем
            execute(photo);

        } catch (Exception e) {
            log.error("Ошибка генерации графика", e);
            sendTextMessage(chatId, "⚠️ Не удалось сгенерировать график");
        }
    }

    private void handleStartCommand(long chatId) {
        if (userService.findByChatId(chatId) == null) {
            log.debug("Новый пользователь: {}", chatId);
            //новый пользователь
            String message = "Добро пожаловать! \n Для работы данного бота необходимо иметь аккаунт в Т Инвестициях и " +
                    "предоставить данному боту токен для взаимодействия с аккаунтом. Для начала работы воспользуйтесь " +
                    "командой " + BotCommand.START.getCommand() + " . \n Если у вас еще нет аккаунта или вы просто хотите" +
                    "посмотреть на работу боту, используйте команду " + BotCommand.SANDBOX.getCommand() + ".";
            sendTextMessage(chatId, message);
            return;
        }

        setStartMenu(chatId, 0);
    }

    private void setStartMenu(long chatId, int messageId, String message) {
        //зарегистрированный пользователь
        InlineKeyboardMarkup keyboard = createStartInlineKeyboard(chatId);

        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        response.setText(message);
        response.setReplyMarkup(keyboard);

        try {
            if (messageId == 0) {
                execute(response);
            } else { // Если не новое сообщение, то перезаписываем
                editMessageText(chatId, messageId, message, keyboard);
            }
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки инлайн-клавиатуры", e);
        }
    }

    private void setStartMenu(long chatId, int messageId) {
        setStartMenu(chatId, messageId, "Выберите действие:");
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
        sendNotification(chatId, null, message);
    }

    public void sendNotification(Long chatId, String message) {
        sendNotification(String.valueOf(chatId), null, message);
    }

    @Override
    public String getBotUsername() {
        return "some_name"; //TODO где используется?
    }

    private void startSandboxCreation(long chatId) {
        startUserCreation(chatId);
        USER_CREATION_STATES.put(chatId, "AWAITING_PHONE_SANDBOX");
    }

    private void startUserCreation(long chatId) {
        USER_CREATION_STATES.put(chatId, "AWAITING_PHONE");

        // Создаем клавиатуру с кнопкой "Поделиться телефоном"
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        // Добавляем клавиатуру
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Создаем кнопку запроса контакта
        KeyboardButton contactButton = new KeyboardButton("Поделиться телефоном");
        contactButton.setRequestContact(true);

        // Создаем ряд и добавляем кнопку
        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);
        keyboard.add(row);

        //Создаем кнопку продолжения
        KeyboardRow continueRow = new KeyboardRow();
        continueRow.add(new KeyboardButton(continueCreationText));
        keyboard.add(continueRow);

        //Создаем кнопку отмены
        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add(new KeyboardButton(cancelCreationText));
        keyboard.add(cancelRow);

        keyboardMarkup.setKeyboard(keyboard);

        // Отправляем сообщение с клавиатурой
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста, поделитесь вашим контактом: \n Вы можете продолжить без заполнения номера телефона" +
                ", но в этом случае вам может быть недоступна часть функциональности бота.");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки запроса телефона: '{}'", e.getMessage());
        }
    }

    private void handleReceivedContact(Message message) {
        long chatId = message.getChatId();
        Contact contact = message.getContact();

        if (!USER_CREATION_STATES.containsKey(chatId)) {
            removeReplyKeyboard(chatId, "❌ Запрос телефона не инициирован. Используйте /create");
            return;
        }

        // Получаем и нормализуем номер телефона
        String phone = normalizePhone(contact.getPhoneNumber());

        // Проверяем номер
        if (!isValidPhone(phone)) {
            removeReplyKeyboard(chatId, "❌ Неверный формат телефона. Пожалуйста, используйте /create снова.");
            return;
        }

        String textMessage = userCreation(message);

        // Скрываем клавиатуру
        removeReplyKeyboard(chatId, textMessage);
        USER_CREATION_STATES.remove(chatId);
    }

    private String userCreation(Message message) {
        Contact contact = message.getContact();
        Chat chat = message.getChat();
        Long chatId = message.getChatId();
        String name, lastName, phone, textMessage;
        TradeStatus tradeStatus;
        UserStatus userStatus;

        if (USER_CREATION_STATES.get(chatId).contains("SANDBOX")) {
            tradeStatus = TradeStatus.SANDBOX;
            userStatus = UserStatus.ACTIVE;
        } else {
            tradeStatus = TradeStatus.READ_ONLY;
            userStatus = UserStatus.NEW;
        }

        User existingUser;

        if (contact != null) {
            phone = normalizePhone(contact.getPhoneNumber());
            name = contact.getFirstName();
            lastName = contact.getLastName();
            existingUser = userService.findByPhone(phone);
        } else {
            phone = null;
            name = chat.getFirstName();
            lastName = chat.getLastName();
            existingUser = userService.findByChatId(chatId);
        }

        if (existingUser != null) {
            textMessage = "ℹ️ Пользователь уже существует.";
        } else {
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setTelegramChatId(message.getChatId());
            newUser.setName(name);
            newUser.setLastName(lastName);
            newUser.setTradeStatus(tradeStatus);
            newUser.setUserStatus(userStatus);
            newUser = userService.createUser(newUser);
            textMessage = "✅ Пользователь " + newUser.getName() + " успешно создан!";
            log.debug("Успешное создание пользователя '{}'", newUser.getId());
        }
        return textMessage;
    }

    // Метод для скрытия клавиатуры
    private void removeReplyKeyboard(long chatId, String textMessage) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        message.setReplyMarkup(keyboardRemove);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка скрытия клавиатуры", e);
        }
    }

    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches() ||
                phone.matches("^7\\d{10}$") ||
                phone.matches("^8\\d{10}$");
    }

    // Нормализация номера телефона
    private String normalizePhone(String phone) {
        // Удаляем все нецифровые символы, кроме плюса
        String normalized = phone.replaceAll("[^+\\d]", "");

        // Заменяем 8 на +7 в начале номера
        if (normalized.startsWith("8") && normalized.length() == 11) {
            return "+7" + normalized.substring(1);
        }

        // Добавляем +7 если номер начинается с 7
        if (normalized.startsWith("7") && normalized.length() == 11) {
            return "+" + normalized;
        }

        return normalized;
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

    public InlineKeyboardMarkup createStartInlineKeyboard(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        UserStatus userStatus = userService.findByChatId(chatId).getUserStatus();
        switch (userStatus) {
            case ACTIVE:
                List<InlineKeyboardButton> activeRow = new ArrayList<>();
                activeRow.add(createInlineKeyboardButton(PORTFOLIO));
                activeRow.add(createInlineKeyboardButton(ANALYTICS));
                keyboard.add(activeRow);
            case NEW:
            default:
                List<InlineKeyboardButton> defaultRow = new ArrayList<>();
                defaultRow.add(createInlineKeyboardButton(SETTINGS));
                keyboard.add(defaultRow);
        }

//        // Второй ряд кнопок
//        List<InlineKeyboardButton> row2 = new ArrayList<>();

//        // Кнопка с URL
//        row2.add(InlineKeyboardButton.builder()
//                .text("Открыть сайт 🌐")
//                .url("https://invest.ru")
//                .build());

//        // Кнопка с веб-приложением
//        row2.add(InlineKeyboardButton.builder()
//                .text("Торговля 💹")
//                .webApp(new WebAppInfo("https://trading.invest.ru"))
//                .build());

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardButton createInlineKeyboardButton(InlineButton inlineButton) {
        return InlineKeyboardButton.builder()
                .text(inlineButton.getLabel())
                .callbackData(inlineButton.getName())
                .build();
    }

    private InlineKeyboardButton createInlineKeyboardButton(String label, String callbackDataName) {
        return InlineKeyboardButton.builder()
                .text(label)
                .callbackData(callbackDataName)
                .build();
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        User user = userService.findByChatId(chatId);
        log.debug("Callback от '{}': '{}'", chatId, callbackData);

        try {
            //TODO сделать валидные ответы
            //Сначала проверяем все точные совпадения
            if (InlineButton.equals(callbackData)) {
                switch (InlineButton.getByName(callbackData)) {
                    case PORTFOLIO:
                        // Обработка запроса портфеля
                        String response1 = "Информация о вашем портфеле " + user.getName();
                        editMessageText(chatId, messageId, response1);
                        String portfolioResponse;
                        try {
                            portfolioResponse = investService.getPortfolio(user);
                        } catch (InvestServiceException e) {
                            portfolioResponse = "Ошибка обработки данных. Пожалуйста проверьте заполненность данных главный " +
                                    "аккаунт и наличие счетов в Т банке";
                            log.error("Ошибка обработки PORTFOLIO", e);
                        }
                        setStartMenu(chatId, messageId, response1 + "\n" + portfolioResponse);
                        break;
                    case ANALYTICS:
                        // Обработка аналитики
                        String response2 = "Аналитические данные " + user.getName();
                        editMessageText(chatId, messageId, response2);
                        String analyticsResponse;
                        try {
                            analyticsResponse = investService.getAnalytics(user);
                        } catch (InvestServiceException e) {
                            analyticsResponse = "Ошибка обработки данных. Пожалуйста проверьте заполненность данных главный " +
                                    "аккаунт и наличие счетов в Т банке";
                            log.error("Ошибка обработки ANALYTICS", e);
                        }
                        setStartMenu(chatId, messageId, response2 + "\n" + analyticsResponse);
                        break;
                    case BACK_TO_MAIN:
                        setStartMenu(chatId, messageId);
                        break;
                    case SETTINGS:
                        String message = "Ваши текущие настройки " + user.getName() + " :\n" + userService.getSettings(user);
                        InlineKeyboardMarkup keyboard = createSettingsInlineKeyboard();

                        try {
                            editMessageText(chatId, messageId, message, keyboard);
                        } catch (TelegramApiException e) {
                            log.error("Ошибка отправки инлайн-клавиатуры для SETTINGS", e);
                        }
                        break;
                    case CHANGE_TRADE_STATUS:
                        changeTradeStatus(chatId, messageId, user);
                        break;
                    default:
                        log.error("Не заполненная кнопка '{}'", InlineButton.getByName(callbackData));
                        sendNotification(chatId, "Едрить - колотить, забыл настроить "
                                + InlineButton.getByName(callbackData).getLabel() + ", не надо нажимать его более");
                }
            } else if (InlineButton.inside(callbackData)) {
                switch (InlineButton.getInlineButtonInside(callbackData)) {
                    case CHANGE_TRADE_STATUS: //TODO сделать все смены и проверить работу
                        // Обработка параметров смены статуса
                        switch (TradeStatus.getTradeStatusInside(callbackData)) {
                            case SANDBOX:
                                user.setTradeStatus(TradeStatus.SANDBOX);
                                sendNotification(chatId,"Статус успешно изменен на " + TradeStatus.SANDBOX);
                                userService.save(user);
                                break;
                            case READ_ONLY, FULL_ACCESS:
                                USER_CHANGE_T_STATUS_STATES.put(chatId,TradeStatus.getTradeStatusInside(callbackData));
                                sendNotification(chatId,"Для подтверждения смены статуса на '" +
                                        TradeStatus.SANDBOX.getName() + "' отправьте следующим сообщением токен из " +
                                        "Т-инвестиций. В случае возникновения ошибок смена статуса будет отменена");
                                break;
                        }
                        break;
                }
            }
            // Подтверждение обработки callback
            answerCallbackQuery(callbackQuery.getId());
            log.debug("Завершение обработки callback от '{}'", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка обработки callback: '{}'", e.getMessage());
        }
    }

    private void changeTradeStatus(long chatId, Integer messageId, User user) {
        StringBuilder message = new StringBuilder();
        message.append("Ваш текущий статус ").append(user.getTradeStatus()).append(".\n Выберите новый статус: ");

        for (TradeStatus tradeStatus : TradeStatus.values()) {
            message.append(tradeStatus.getLabel()).append(" : ").append(tradeStatus.getDescription());
        }

        InlineKeyboardMarkup keyboard = createChangeTradeStatusInlineKeyboard();

        try {
            editMessageText(chatId, messageId, message.toString(), keyboard);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки инлайн-клавиатуры для CHANGE_USER_STATUS : '{}'", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createSettingsInlineKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineKeyboardButton(CHANGE_USER_STATUS));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineKeyboardButton(CHANGE_TRADE_STATUS));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineKeyboardButton(CLOSE_ACCOUNT));
        row3.add(createInlineKeyboardButton(BACK_TO_MAIN));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup createChangeTradeStatusInlineKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (TradeStatus tradeStatus : TradeStatus.values()) {
            keyboard.add(List.of(createInlineKeyboardButton(tradeStatus.getLabel(),
                    CHANGE_TRADE_STATUS.getName() + tradeStatus.getName())));
        }

        List<InlineKeyboardButton> row3 = new ArrayList<>();
//        row3.add(createInlineKeyboardButton(CLOSE_ACCOUNT));
        // возврат на предыдущий экран //TODO реализовать значение в дереве или еще как?
        row3.add(createInlineKeyboardButton("Назад", SETTINGS.getName()));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private void editMessageText(long chatId, int messageId, String newText, InlineKeyboardMarkup inlineKeyboardMarkup) throws TelegramApiException {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        if (inlineKeyboardMarkup != null) {
            editMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        execute(editMessage);
    }

    private void editMessageText(long chatId, int messageId, String newText) throws TelegramApiException {
        editMessageText(chatId, messageId, newText, null);
    }


    private void answerCallbackQuery(String callbackId) throws TelegramApiException {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        execute(answer);
    }

}