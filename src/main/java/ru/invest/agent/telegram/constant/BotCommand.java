package ru.invest.agent.telegram.constant;

import lombok.Getter;

@Getter
public enum BotCommand {
    INFO("/info", ""),
    PING("/ping", "Проверка доступности бота"),
    CREATE("/create", "Создание аккаунта"),
    DELETE("/delete", "Удаление аккаунта"),
    START("/start", "Запуск бота"),
    CHART_EXAMPLE("/chartExample", "");

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

