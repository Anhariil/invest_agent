package ru.invest.agent.api.service;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Value;
import ru.invest.agent.api.BaseTest;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.InvestAccount;
import ru.invest.agent.api.utils.InvestApiUtils;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Разрешаем нестатические @*All
public class InvestServiceTest extends BaseTest {

    @Value("${invest_api.token.sand_box}")
    private String sandBoxToken;
    @Value("${invest_api.token.account_id}")
    private String accountId;

    private final TradeStatus tradeStatus = TradeStatus.SANDBOX;
    private InvestApi investApi;

    @InjectMocks
    private InvestApiService investApiService;

    @BeforeAll
    public void init() {

        investApi = InvestApiUtils.createInvestApi(sandBoxToken, tradeStatus);
        investApiService = new InvestApiService(investApi);

        //на всякий случай дополнительно запускаем очистку
//        clearAssets();
        if (investApiService.isAccountHasMoney(accountId)){
        investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(100000).setCurrency("RUB").build());
        }
    }

//    @BeforeEach
//    public void setMoney() {
//        investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(100000).setCurrency("RUB").build());
//    }
//
//    @AfterEach
//    public void clearAssets() {
//        var moneyList = investApiService.getAccountsMoneyList(accountId);
//
//        //TODO очищаем счет от инструментов
//
//        //очищаем счет от денег
//        for (Money moneyValue : moneyList) {
//            investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder()
//                    .setUnits(-moneyValue.getValue().longValue())
//                    .setCurrency(moneyValue.getCurrency()).build());
//        }
//
//    }

    @Test
    public void should_fail_when_api_is_null() {
        InvestApiService investApiService1 = new InvestApiService(null);
        Exception ex = Assertions.assertThrows(Exception.class,
                () -> investApiService1.createOrUpdateOrders(null, null));
        assertEquals("Ошибка соединения", ex.getMessage());
    }

    @Test
    public void should_return_money_from_account() {
        assertFalse(InvestApiService.getAccountsMoneyList(investApi, accountId).isEmpty());

        Money money = InvestApiService.getAccountsMoney(investApi, accountId, "rub");

        assertTrue(Objects.nonNull(money));
        assertEquals("rub", money.getCurrency());
        assertTrue(money.getValue().doubleValue() > 0.0); //?
    }

    @Test
    public void should_pass_when_account_is_valid() {
        InvestAccount investAccount = new InvestAccount();
        investAccount.setId(accountId);

        assertDoesNotThrow(() -> investApiService.createOrUpdateOrders(List.of(investAccount), tradeStatus));
    }


    @Test
    public void get_shares_last_price_test(){
        var shares = InvestApiService.getAllAvailableShares(investApi);
        var sharesLastPrice =  InvestApiService.getSharesLastPrice(investApi,
                shares.stream().map(Share::getFigi).reduce("", (a,b) -> a + b +";"));
        assertFalse(sharesLastPrice.isEmpty());

        var lotMap = shares.stream().collect(Collectors.toMap(Share::getFigi, Share::getLot));

        double priceLast = InvestApiService.quotationToDouble(sharesLastPrice.getLast().getPrice())
                * lotMap.get(sharesLastPrice.getLast().getFigi());
        double priceFirst= InvestApiService.quotationToDouble(sharesLastPrice.getFirst().getPrice())
                * lotMap.get(sharesLastPrice.getFirst().getFigi());
        assertTrue(priceLast > priceFirst);

//        for (var shareLastPrice : sharesLastPrice){
//            System.out.println("price: " + InvestApiService.quotationToDouble(shareLastPrice.getPrice())
//                    + " lot: " + lotMap.get(shareLastPrice.getFigi()));
//        }
    }
}
