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
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.telegram.constant.BotCommand;
import ru.invest.agent.telegram.constant.InlineButton;
import ru.invest.agent.telegram.utils.ChartGenerator;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

import static ru.invest.agent.telegram.constant.BotCommand.PING;
import static ru.invest.agent.telegram.constant.InlineButton.*;

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

        // Обработка получения контакта
        if (update.hasMessage() && update.getMessage().hasContact()) {
            handleReceivedContact(update.getMessage());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        // Обработка команд / полученного текста
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        log.debug("сообщение от: '{}',текст: '{}'", chatId, messageText);

        //Обработка текстовых команд
        if ("Отмена".equals(messageText) && USER_CREATION_STATES.containsKey(chatId)) {
            removeReplyKeyboard(chatId, "❌ Создание пользователя отменено");
            USER_CREATION_STATES.remove(chatId);
            return;
        }

        if (BotCommand.contains(messageText)) {
            BotCommand command = BotCommand.getByCommand(messageText);
            // Обработка команд
            switch (command) {
                case PING:
                    sendTextMessage(chatId, "pong");
                    break;
                case START:
                    handleStartCommand(chatId);
                    break;
                case CREATE:
                    startUserCreation(chatId);
                    break;
                case CHART_EXAMPLE:
                    sendBarChartExample(chatId);
                    break;
            }
        } else {
            sendTextMessage(chatId, "Неизвестная команда. Используйте /info для получения информации по командам.");
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
            byte[] chartImage = ChartGenerator.generateSimplePieChart("Пример графика", new String[]{"Актив А","Актив Б","Актив Е"}
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
        String message = "Выберите действие:";
        InlineKeyboardMarkup keyboard = createStartInlineKeyboard(chatId);

        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        response.setText(message);
        response.setReplyMarkup(keyboard);

        try {
            execute(response);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки инлайн-клавиатуры", e);
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
        sendNotification(chatId, null, message);
    }

    @Override
    public String getBotUsername() {
        return "some_name"; //TODO где используется?
    }

    private void startUserCreation(long chatId) {
        USER_CREATION_STATES.put(chatId, "AWAITING_PHONE");

        // Создаем клавиатуру с кнопкой "Поделиться телефоном"
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        // Создаем кнопку запроса контакта
        KeyboardButton contactButton = new KeyboardButton("Поделиться телефоном");
        contactButton.setRequestContact(true);

        // Создаем ряд и добавляем кнопку
        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);

        //Создаем кнопку отмены
        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add(new KeyboardButton("Отмена"));

        // Добавляем ряд в клавиатуру
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboard.add(cancelRow);
        keyboardMarkup.setKeyboard(keyboard);

        // Отправляем сообщение с клавиатурой
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста, поделитесь вашим контактом:");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки запроса телефона", e);
        }
    }

    private void handleReceivedContact(Message message) {
        long chatId = message.getChatId();
        Contact contact = message.getContact();

        if (!USER_CREATION_STATES.containsKey(chatId)) {
            sendTextMessage(chatId, "❌ Запрос телефона не инициирован. Используйте /create");
            return;
        }

        // Получаем и нормализуем номер телефона
        String phone = normalizePhone(contact.getPhoneNumber());

        // Проверяем номер
        if (!isValidPhone(phone)) {
            removeReplyKeyboard(chatId, "❌ Неверный формат телефона. Пожалуйста, используйте /create снова.");
            return;
        }

        // Создаем пользователя
        User existingUser = userService.findByPhone(phone);
        String textMessage;
        if (existingUser != null) {
            textMessage = "ℹ️ Пользователь с телефоном уже существует.";
        } else {
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setTelegramChatId(chatId);
            newUser.setName(contact.getFirstName());
            newUser.setLastName(contact.getLastName());
            newUser = userService.createUser(newUser);
            textMessage = "✅ Пользователь " + newUser.getName() + " успешно создан!";
        }

        // Скрываем клавиатуру
        removeReplyKeyboard(chatId, textMessage);
        USER_CREATION_STATES.remove(chatId);
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

        // Первый ряд кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        row1.add(createInlineKeyboardButton(PORTFOLIO));
        row1.add(createInlineKeyboardButton(ANALYTICS));

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

        // Третий ряд (одна кнопка)
        List<InlineKeyboardButton> row3 = new ArrayList<>();

        row3.add(createInlineKeyboardButton(SETTINGS));

        // Собираем все ряды
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
//        keyboard.add(row2);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardButton createInlineKeyboardButton(InlineButton inlineButton) {
        return InlineKeyboardButton.builder()
                .text(inlineButton.getLabel())
                .callbackData(inlineButton.getName())
                .build();
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.debug("Callback от '{}': '{}'", chatId, callbackData);

        try {
            switch (InlineButton.getByName(callbackData)){
                case PORTFOLIO:
                    // Обработка запроса портфеля
                    String response1 = "Информация о вашем портфеле...";
                    editMessageText(chatId, messageId, response1); //TODO сделать валидные ответы
                    break;
                case ANALYTICS:
                    // Обработка аналитики
                    String response2 = "Аналитические данные...";
                    sendTextMessage(chatId, response2);
                    break;
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