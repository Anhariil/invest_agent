package ru.invest.agent.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.protobuf.Timestamp;
import lombok.Data;
import ru.invest.agent.api.constant.AssetsConstant;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.models.Money;

import java.util.ArrayList;
import java.util.List;

@Data
public class InvestAccount {
    private boolean isAvailable;
    private String id; // номер аккаунта для связи
    @JsonIgnore
    private Timestamp createDateTime;
    @JsonIgnore
    private Timestamp closeDateTime;
    @JsonIgnore
    private Timestamp lastUpdateDateTime;
    private boolean isMarginTrading = false;
    private Assets assets = new Assets();
    private List<AssetsConstant> availableAssets = new ArrayList<>(); // выбранные услуги для торговли
//    @JsonIgnore
    private List<Money> monies;

    public InvestAccount createFromTinkoff(Account tinkoffAccount) {
        InvestAccount account = new InvestAccount();
//       account.setAvailable(tinkoffAccount.isInitialized()); //TODO проверить
        account.setId(tinkoffAccount.getId());
        account.setCreateDateTime(tinkoffAccount.getOpenedDate());
        account.setCloseDateTime(tinkoffAccount.getClosedDate());
//        lastUpdateDateTime = //TODO как получить дату в момент обновления
//        account.setMarginTrading(tinkoffAccount.); //TODO как получить (если он есть) признак марж торговли
//       account.setMonies(); //TODO а надо ли
        return account;
    }
}
