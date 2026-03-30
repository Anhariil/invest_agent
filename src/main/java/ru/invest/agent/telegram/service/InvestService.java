package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.invest.agent.api.constant.AssetsConstant;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.AccountHistory;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.InvestApiService;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.api.utils.InvestApiUtils;
import ru.invest.agent.telegram.exception.InvestServiceException;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Position;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InvestService {

    @Autowired
    UserService userService;

    //TODO сделать аналитику (изменения за день / неделю ?)
    public String getAnalytics(User user) throws InvestServiceException {
        String token = user.getToken();
        TradeStatus tradeStatus = user.getTradeStatus();
        InvestApi investApi = InvestApiUtils.createInvestApi(token, tradeStatus);
        InvestApiService investApiService = new InvestApiService(investApi);

        StringBuilder result = new StringBuilder();
        try {
            String accountId;
            if (StringUtils.isNotEmpty(user.getMainAccount())) {
                accountId = user.getMainAccount();
            } else if (StringUtils.isNotEmpty(InvestApiUtils.getAccountsList(investApi).getFirst().getId())) {
                throw new InvestServiceException("Не выбран главный счет, userid: " + user.getId());
            } else {
                throw new InvestServiceException("Нет счетов в Т, userid: " + user.getId());
            }

            List<Money> moneys = investApiService.getAccountsMoneyList(accountId);
            for (Money money : moneys) {
                result.append(money.getValue().doubleValue())
                        .append(" ")
                        .append(money.getCurrency())
                        .append("\n");
            }
            user.setAnalyticsData(result.toString(),Instant.now());
        } catch (ApiRuntimeException e) {
            log.warn("Ошибка получения аналитики: '{}'", e.getMessage());

            if (Optional.ofNullable(user.getAccountHistory()).map(AccountHistory::getAnalyticsData).isPresent()) {
                result.append("Состояние счета на дату: ")
                        .append(user.getAccountHistory().getAnalyticsLastUpdateDate()).append("\n")
                        .append(user.getAccountHistory().getAnalyticsData());
            } else {
                result.append("Ошибка получения портфолио, пожалуйста, попробуйте позже");
            }
        } finally {
            userService.save(user);
        }
        return result.toString();
    }

    public String getPortfolio(User user) throws InvestServiceException {
        String token = user.getToken();
        TradeStatus tradeStatus = user.getTradeStatus();
        InvestApi investApi = InvestApiUtils.createInvestApi(token, tradeStatus);
        InvestApiService investApiService = new InvestApiService(investApi);

        String accountId;
        if (StringUtils.isNotEmpty(user.getMainAccount())) {
            accountId = user.getMainAccount();
        } else if (StringUtils.isNotEmpty(InvestApiUtils.getAccountsList(investApi).getFirst().getId())) {
            throw new InvestServiceException("Не выбран главный счет, userid: " + user.getId());
        } else {
            throw new InvestServiceException("Нет счетов в Т, userid: " + user.getId());
        }

        StringBuilder result = new StringBuilder();
        try {
            List<Position> positions = investApiService.getPositionList(accountId).stream()
                    // TODO проверить, нужна ли сортировка
                    .sorted((p1, p2) -> StringUtils.compare(p1.getInstrumentType(), p2.getInstrumentType()))
                    .toList();
            for (Position position : positions) {
                AssetsConstant asset = AssetsConstant.getAssetsByName(position.getInstrumentType());
                if (result.indexOf(asset.getTranslatedName()) == -1) {
                    result.append("\n").append(asset.getTranslatedName()).append(": \n");
                }
                result.append(convertPositionToString(investApi, position)).append("\n");
            }
            user.setPortfolioData(result.toString(), Instant.now());
        } catch (ApiRuntimeException e) {
            log.warn("Ошибка получения портфолио: '{}'", e.getMessage());
            result = new StringBuilder();
            if (Optional.ofNullable(user.getAccountHistory()).map(AccountHistory::getPortfolioData).isPresent()) {
                result.append("Состояние портфеля на дату: ")
                        .append(user.getAccountHistory().getPortfolioLastUpdateDate()).append("\n")
                        .append(user.getAccountHistory().getPortfolioData());
            } else {
                result.append("Ошибка получения портфолио, пожалуйста, попробуйте позже");
            }
        } finally {
            userService.save(user);
        }
        return result.toString();
    }

    private String convertPositionToString(InvestApi investApi, Position position) {
        String name = InvestApiUtils.getInstrumentName(investApi, position.getFigi(),
                AssetsConstant.getAssetsByName(position.getInstrumentType()));
//        double price = position.getQuantity().doubleValue();
        //TODO Проверить валидность
        double price;
        if (position.getInstrumentType().equals("currency")) {
            price = position.getQuantity().doubleValue();
        } else {
            price = position.getQuantity().multiply(position.getAveragePositionPrice().getValue()).doubleValue();
        }
        String currency = position.getCurrentPrice().getCurrency();
        switch (currency) {
            case "rub" -> currency = "рублей";
            case "usd" -> currency = "долларов";
            case "cny" -> currency = "юаней";
        }
        return name + " " + price + " " + currency;
    }

//    private String convertMoneyToString(Money money){
//        return InvestApiService.moneyValueToDouble(money);
//    }

    public boolean isTokenValid(String token, TradeStatus tradeStatus){
        boolean result;
        try{
            InvestApi investApi = InvestApiUtils.createInvestApi(token, tradeStatus);
            //вызываем метод для проверки, что нет ошибок
            InvestApiUtils.getAccountsList(investApi);
            result = true;
        } catch (ApiRuntimeException e){
            result = false;
        }
        return result;
    }
}
