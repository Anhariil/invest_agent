package ru.invest.agent.api.model;

import lombok.Data;

@Data
public class TwoValues<T,E> {

    private T firstValue;
    private E secondValue;

    public TwoValues(T value1, E value2){
        firstValue = value1;
        secondValue = value2;
    }
}
