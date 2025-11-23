package ru.invest.agent.api.constant;

import lombok.Getter;
import ru.invest.agent.telegram.constant.BotCommand;

@Getter
public enum AssetsConstant {

    CURRENCY("currency", "валюта" ),
    SHARE("share", "акции" ),
    BOND("bond", "облигации" ),
    FUTURE("future", "фьючерсы" ),
    OPTION("option", "опционы" );

    final String name;
    final String translatedName;

    AssetsConstant(String name, String translatedName){
        this.name = name;
        this.translatedName =translatedName;
    }

    public static boolean contains(String name) {
        for (AssetsConstant assetsConstant : AssetsConstant.values()) {
            if (assetsConstant.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static AssetsConstant getAssetsByName(String name){
        if (contains(name)){
            for (AssetsConstant assetsConstant : AssetsConstant.values()) {
                if (assetsConstant.getName().equals(name)) {
                    return assetsConstant;
                }
            }
        }
        throw new RuntimeException("Ошибка в AssetsConstant.getAssetsByName : невалидное имя " + name);
    }
}