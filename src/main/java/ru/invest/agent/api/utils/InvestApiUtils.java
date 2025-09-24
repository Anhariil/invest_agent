package ru.invest.agent.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.invest.agent.api.constant.TradeStatus;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.Objects;

@Slf4j
public class InvestApiUtils {

    public static InvestApi createInvestApi(String token, TradeStatus tradeStatus){
        if (StringUtils.isBlank(token) || Objects.isNull(tradeStatus)){
            log.error("Пустой токен или статус");
            return null;
        }

        switch (tradeStatus){
            case SANDBOX -> {
                return InvestApi.createSandbox(token);
            }
            case READ_ONLY -> {
                return InvestApi.createReadonly(token);
            }
            case FULL_ACCESS -> {
                return InvestApi.create(token);
            }
            default -> {
                log.error("Ошибка в типе TradeStatus: '{}'",tradeStatus);
                return null;
            }
        }
    }
}
