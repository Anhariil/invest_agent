package ru.invest.agent.telegram.constant;

import lombok.Getter;

@Getter
public enum InlineButton {
    PORTFOLIO("Portfolio", "Портфель 📊"),
    ANALYTICS("Analytics", "Аналитика 📈"),
    SETTINGS("Settings","Настройки ⚙️"),
    BACK("Back","Назад");

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
