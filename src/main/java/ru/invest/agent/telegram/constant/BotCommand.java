package ru.invest.agent.telegram.constant;

import lombok.Getter;

@Getter
public enum BotCommand {
    //Default commands
    START("/start", "Запуск бота"),
    HELP("/help", "help"),
    STOP("/stop", "Остановка работы бота"),
    //Functional commands
    CREATE("/create", "Создание аккаунта"),
    SANDBOX("/sandbox", "Создание аккаунта в песочнице"),
    SETTINGS("/settings","Настройки аккаунта"),
    DELETE("/delete", "Удаление аккаунта"),
    //Test commands
    PING("/ping", "Проверка доступности бота"),
    CHART_EXAMPLE("/chartexample", "chartExample");

    private final String command;
    private final String description;

    BotCommand(String text, String description) {
        this.command = text;
        this.description = description;
    }

    public static boolean contains(String command) {
        for (BotCommand botCommand : BotCommand.values()) {
            if (botCommand.getCommand().equals(command)) {
                return true;
            }
        }
        return false;
    }

    public static BotCommand getByCommand(String command) {
        if (contains(command)) {
            for (BotCommand botCommand : BotCommand.values()) {
                if (botCommand.getCommand().equals(command)) {
                    return botCommand;
                }
            }
        }
        throw new RuntimeException("Ошибка в BotCommand.getByCommand : невалидная команда " + command);
    }
}

