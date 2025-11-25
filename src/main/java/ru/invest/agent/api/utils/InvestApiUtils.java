package ru.invest.agent.api.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import ru.invest.agent.api.constant.AssetsConstant;
import ru.invest.agent.api.constant.TradeStatus;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class InvestApiUtils {

    //TODO проверить баги когда один участник удалил сессию, а второй обращается без проверки на существование
    private static final Map<String,InvestApi> investApiList = new HashMap<>();

    @Value("${invest_api.token.sand_box}")
    private String sandBoxToken;

    @NonNull
    public static InvestApi createInvestApi(String token, TradeStatus tradeStatus){
        if (StringUtils.isBlank(token) || Objects.isNull(tradeStatus)){
            log.error("Пустой токен или статус");
            throw new RuntimeException();
        }

        if (investApiList.get(token) != null){
            return investApiList.get(token);
        }

        InvestApi investApi;

        switch (tradeStatus){
            case SANDBOX -> {
                investApi = InvestApi.createSandbox(token);
            }
            case READ_ONLY -> {
                investApi = InvestApi.createReadonly(token);
            }
            case FULL_ACCESS -> {
                investApi = InvestApi.create(token);
            }
            default -> {
                log.error("Ошибка в типе TradeStatus: '{}'",tradeStatus);
                throw new RuntimeException();
            }
        }

        investApiList.put(token,investApi);
        return investApi;
    }

    public static List<Account> getAccountsList(InvestApi investApi){
        return investApi.getUserService().getAccountsSync();
    }

    /**
     * Получаем название инструмента через его figi
     * @param investApi контекст для вызова
     * @param figi название инструмента
     * @param assets тип инструмента
     * @return
     */
    public static String getInstrumentName(InvestApi investApi, String figi, AssetsConstant assets){
        return switch (assets) {
            case BOND -> investApi.getInstrumentsService().getBondByFigiSync(figi).getName();
            case CURRENCY -> investApi.getInstrumentsService().getCurrencyByFigiSync(figi).getName();
            case SHARE -> investApi.getInstrumentsService().getShareByFigiSync(figi).getName();
            case FUTURE -> investApi.getInstrumentsService().getFutureByFigiSync(figi).getName();
            case OPTION -> investApi.getInstrumentsService().getOptionByPositionUidSync(figi).getName();
            case null, default -> throw new RuntimeException("ошибка обработки имени инструмента");//?
        };
    }

    public static String createTestAccount(String token, TradeStatus tradeStatus){
        InvestApi investApi = createInvestApi(token,tradeStatus);
        String accountId = investApi.getSandboxService().openAccountSync();
        // Заполняем аккаунт тестовой суммой
        investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(100000).setCurrency("RUB").build());
        return accountId;
    }

    public static String getSandboxToken(){
        return new InvestApiUtils().sandBoxToken;
    }

    public static void destroy(String token, int second) {
        investApiList.get(token).destroy(second);
        investApiList.remove(token);
    }
}
