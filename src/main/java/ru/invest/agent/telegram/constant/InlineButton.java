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
        if (equals(name)){
            for (InlineButton inlineButton : InlineButton.values()){
                if (inlineButton.getName().equals(name)){
                    return inlineButton;
                }
            }
        }
        throw new RuntimeException("Ошибка в InlineButton.getByName : невалидное название " + name);
    }

    public static boolean equals(String name){
        for (InlineButton inlineButton : InlineButton.values()){
            if (inlineButton.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращаем {@code InlineButton} с которого начинается name
     * @param name параметр, который должен начинаться с одной из констант {@code InlineButton}. Если он начинается с
     *             других символов, то метод вернет ошибку {@code RuntimeException}
     * @return
     */
    public static InlineButton getInlineButtonInside(String name){
        if (inside(name)){
            for (InlineButton inlineButton : InlineButton.values()){
                if (name.startsWith(inlineButton.getName())){
                    return inlineButton;
                }
            }
        }
        throw new RuntimeException("Ошибка в InlineButton.getByName : невалидное название " + name);
    }

    public static boolean inside(String name){
        for (InlineButton inlineButton : InlineButton.values()){
            if (name.startsWith(inlineButton.getName())){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
