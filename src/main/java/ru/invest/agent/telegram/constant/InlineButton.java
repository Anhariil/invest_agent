package ru.invest.agent.telegram.constant;

import lombok.Getter;

@Getter
public enum InlineButton {
    PORTFOLIO("Portfolio", "Портфель 📊"),
    ANALYTICS("Analytics", "Аналитика 📈"),
    SETTINGS("Settings","Настройки ⚙️"),
    SET_TOKEN("SetToken","Ввести токен"),
    CHANGE_USER_STATUS("ChangeUserStatus","Изменить статус пользователя"),
    CHANGE_TRADE_STATUS("ChangeTradeStatus","Изменить статус торговли бота"),
    CLOSE_ACCOUNT("CloseAccount","Удалить аккаунт(не Т а бота)"),
    BACK_TO_MAIN("BackToMain","Назад");

    private final String name;
    private final String label;

    InlineButton(String name, String label){
        this.name = name;
        this.label = label;
    }

    public static InlineButton getByName(String name){
        if (contains(name)){
            for (InlineButton inlineButton : InlineButton.values()){
                if (inlineButton.getName().equals(name)){
                    return inlineButton;
                }
            }
        }
        throw new RuntimeException("Ошибка в InlineButton.getByName : невалидное название " + name);
    }

    public static boolean contains(String name){
        for (InlineButton inlineButton : InlineButton.values()){
            if (inlineButton.getName().equals(name)){
                return true;
            }
        }
        return false;
    }
}
