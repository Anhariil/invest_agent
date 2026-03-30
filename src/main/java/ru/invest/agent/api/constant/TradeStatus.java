package ru.invest.agent.api.constant;

import lombok.Getter;
import ru.invest.agent.telegram.constant.InlineButton;

@Getter
public enum TradeStatus {
    SANDBOX("Sandbox","Песочница","Данный статус предназначен для ознакомления с работой бота, все операции " +
            "происходят на копии данных с московской биржи и не влияют ваш портфель."),
    READ_ONLY("ReadOnly","Доступ на чтение", "Доступ только на просмотр данных аккаунта, без возможности ведения " +
            "автоматической торговли ботом."),
    FULL_ACCESS("FullAccess","Полный доступ", "Данный статус предназначен для работы бота с вашим аккаунтом. Для " +
            "переключения на данный статус необходимо будет предоставить токен из T-инвестиций для подключения.");

    private final String name;
    private final String label;
    private final String description;

    TradeStatus(String name, String label, String description){
        this.name = name;
        this.label = label;
        this.description = description;
    }

    public static TradeStatus getTradeStatusInside(String name){
        if (inside(name)){
            for (TradeStatus tradeStatus : TradeStatus.values()){
                if (name.contains(tradeStatus.getName())){
                    return tradeStatus;
                }
            }
        }
        throw new RuntimeException("Ошибка в InlineButton.getByName : невалидное название " + name);
    }

    public static boolean inside(String name){
        for (TradeStatus tradeStatus : TradeStatus.values()){
            if (name.contains(tradeStatus.getName())){
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
