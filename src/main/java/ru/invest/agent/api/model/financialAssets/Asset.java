package ru.invest.agent.api.model.financialAssets;

import lombok.Data;

@Data
public abstract class Asset {

    private final int INSTRUMENT_LIMIT = 5; //TODO переписывать настройками счета? глобальная переменная?
    private String figi;
    private String currency;
    private String lot;
    private String name;
    private String sector;
}
