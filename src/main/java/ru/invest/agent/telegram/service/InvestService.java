package ru.invest.agent.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.InvestApiService;
import ru.invest.agent.api.utils.InvestApiUtils;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class InvestService {


    public String getAnalytics(User user) {
        String token = user.getToken();
        TradeStatus tradeStatus = user.getTradeStatus();
        InvestApi investApi = InvestApiUtils.createInvestApi(token,tradeStatus);
        InvestApiService investApiService = new InvestApiService(investApi);

        String accountId;
        if (StringUtils.isNotEmpty(user.getMainAccount())){
            accountId = user.getMainAccount();
        } else if (StringUtils.isNotEmpty(InvestApiUtils.getAccountsList(investApi).getFirst().getId())) {
            throw new RuntimeException("Нет главного счета");
        } else {
            throw new RuntimeException("Нет счетов в Т");
        }

        List<Money> moneys = investApiService.getAccountsMoneyList(accountId);
        return "";
    }
}
