package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.invest.agent.api.constant.AssetsConstant;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.InvestApiService;
import ru.invest.agent.api.utils.InvestApiUtils;
import ru.invest.agent.telegram.exception.InvestServiceException;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Position;

import java.util.List;

@Slf4j
@Service
public class InvestService {

    //TODO сделать аналитику (изменения за день / неделю ?)
    public String getAnalytics(User user) throws InvestServiceException {
        String token = user.getToken();
        TradeStatus tradeStatus = user.getTradeStatus();
        InvestApi investApi = InvestApiUtils.createInvestApi(token,tradeStatus);
        InvestApiService investApiService = new InvestApiService(investApi);

        String accountId;
        if (StringUtils.isNotEmpty(user.getMainAccount())){
            accountId = user.getMainAccount();
        } else if (StringUtils.isNotEmpty(InvestApiUtils.getAccountsList(investApi).getFirst().getId())) {
            throw new InvestServiceException("Не выбран главный счет, userid: " + user.getId());
        } else {
            throw new InvestServiceException("Нет счетов в Т, userid: " + user.getId());
        }

        StringBuilder result = new StringBuilder();
        List<Money> moneys = investApiService.getAccountsMoneyList(accountId);
        for (Money money: moneys){
            result.append(money.getValue().doubleValue())
                    .append(" ")
                    .append(money.getCurrency())
                    .append("\n");
        }
        return result.toString();
    }

    public String getPortfolio(User user) throws InvestServiceException {
        String token = user.getToken();
        TradeStatus tradeStatus = user.getTradeStatus();
        InvestApi investApi = InvestApiUtils.createInvestApi(token,tradeStatus);
        InvestApiService investApiService = new InvestApiService(investApi);

        String accountId;
        if (StringUtils.isNotEmpty(user.getMainAccount())){
            accountId = user.getMainAccount();
        } else if (StringUtils.isNotEmpty(InvestApiUtils.getAccountsList(investApi).getFirst().getId())) {
            throw new InvestServiceException("Не выбран главный счет, userid: " + user.getId());
        } else {
            throw new InvestServiceException("Нет счетов в Т, userid: " + user.getId());
        }

        StringBuilder result = new StringBuilder();
        List<Position> positions = investApiService.getPositionList(accountId).stream()
                // TODO проверить, нужна ли сортировка
                .sorted( (p1,p2) -> StringUtils.compare(p1.getInstrumentType(), p2.getInstrumentType()))
                .toList();
        for (Position position: positions){
            AssetsConstant asset = AssetsConstant.getAssetsByName(position.getInstrumentType());
            if (result.indexOf(asset.getTranslatedName()) == -1 ){
                result.append("\n").append(asset.getTranslatedName()).append(": \n");
            }
            result.append(convertPositionToString(investApi, position)).append("\n");
        }
        return result.toString();
    }

    private String convertPositionToString(InvestApi investApi, Position position){
        String name = InvestApiUtils.getInstrumentName(investApi, position.getFigi(),
                AssetsConstant.getAssetsByName(position.getInstrumentType()));
//        double price = position.getQuantity().doubleValue();
        //TODO Проверить валидность
        double price;
        if (position.getInstrumentType().equals("currency")){
            price = position.getQuantity().doubleValue();
        } else {
            price = position.getQuantity().multiply(position.getAveragePositionPrice().getValue()).doubleValue();
        }
        String currency = position.getCurrentPrice().getCurrency();
        switch (currency){
            case "rub" -> currency = "рублей";
            case "usd" -> currency = "долларов";
            case "cny" -> currency = "юаней";
        }
        return name + " " + price + " " + currency;
    }

//    private String convertMoneyToString(Money money){
//        return InvestApiService.moneyValueToDouble(money);
//    }
}
