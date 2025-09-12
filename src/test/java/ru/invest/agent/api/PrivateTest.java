package ru.invest.agent.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ru.invest.agent.InvestAgentApplication;
import ru.invest.agent.api.service.AdvancedPredictionService;
import ru.invest.agent.api.utils.JsonConverter;
import ru.invest.agent.api.utils.JsonFileWriter;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.core.models.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;
import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;

@Slf4j
//@ExtendWith(MockitoExtension.class)
@SpringBootTest()
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Разрешаем нестатические @BeforeAll
public class PrivateTest {

    @Value("${file.path}")
    private String testPath;
    @Value("${invest_api.token.sand_box}")
    private String sandBoxToken;
    private InvestApi investApi;
    @Value("${invest_api.token.account_id}")
    private String accountId;

    @Autowired
    private Environment env;

    //создаем тестовый аккаунт для проверки методов
    @BeforeAll
    public void createTestAccount(){

        log.info(accountId);
        Objects.requireNonNull(sandBoxToken, "Sandbox token must not be null");
        Objects.requireNonNull(accountId, "Account ID must not be null");

        investApi = InvestApi.createSandbox(sandBoxToken);
        log.info("Создан экземпляр API в режиме Sandbox");

        assertFalse(investApi.getSandboxService().getAccountsSync().stream().filter(account -> account.getId().equals(accountId)).toList().isEmpty());
        log.info("Выбран счет с id = {}", accountId);

//        assertEquals(1,investApi.getSandboxService().getAccountsSync().size());
    }

//    //удаляем счета если они остались
//    private static void clearAccounts(InvestApi investApi) {
//        var accounts = investApi.getSandboxService().getAccountsSync();
//        if (!accounts.isEmpty()) {
//            log.info("Очистка старых счетов нужна");
//            for (var account : accounts) {
//                investApi.getSandboxService().closeAccountSync(account.getId());
//            }
//        } else {log.info("Очистка старых счетов не нужна");}
//    }

    //очищаем тестовый аккаунт для проверки методов
    @AfterAll
    public void deleteTestAccount(){
//        investApi.getSandboxService().closeAccountSync(accountId);
//        assertFalse(investApi.getSandboxService().getAccountsSync().stream()
//                .anyMatch(a -> a.getId().equals(accountId)));
//        log.info("Закрыт новый счет с id = {}", accountId);
        var moneyList = investApi.getOperationsService().getPositionsSync(accountId).getMoney();

        //очищаем счет от деняк
        for (Money moneyValue : moneyList) {

            investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder()
                    .setUnits(moneyValue.getValue().longValue())
                    .setCurrency(moneyValue.getCurrency()).build());
        }
    }

    @Test
    public void isSandboxMode(){
        log.info("Тип счета - sandbox == {}",investApi.isSandboxMode());
        assertTrue(investApi.isSandboxMode());
    }

    @Test
    public void addSomeMoneyOnTestAccount(){
        investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(10000).setCurrency("RUB").build());
        investApi.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(10000).setCurrency("USD").build());
        var positions = investApi.getOperationsService().getPositionsSync(accountId);

        log.info("список валютных позиций портфеля");
        var moneyList = positions.getMoney();
        for (Money moneyValue : moneyList) {
            log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue.getValue());
            assertTrue(
                    moneyValue.getValue().subtract(new BigDecimal("10000.00")).abs().compareTo(new BigDecimal("0.01")) <= 0,
                    "Разница деняк превышает допустимую погрешность"
            );
        }
    }

    @Test
    public void getReadSomeFigi(){

        //TODO моки на getAllSharesSync() getCandlesSync() ?
        var allShares = investApi.getInstrumentsService().getAllSharesSync().stream()
                .filter(s -> !s.getForQualInvestorFlag() && s.getApiTradeAvailableFlag()
                        && s.getBuyAvailableFlag() && s.getSellAvailableFlag())
                .toList();

        List<HistoricCandle> priceList = investApi.getMarketDataService().getCandlesSync(allShares.getFirst().getFigi(), Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                CandleInterval.CANDLE_INTERVAL_1_MIN).reversed();
        log.info("время первой свечи {}", timestampToString(priceList.getFirst().getTime()));
        log.info("время последней свечи {}", timestampToString(priceList.getLast().getTime()));

        String json =  JsonConverter.convertObjectToJson(priceList);
        JsonFileWriter.saveJson(json,testPath + "/priceList.json");

        AdvancedPredictionService predictionService = new AdvancedPredictionService();
        double[] shortTermPrices = priceList.stream().limit(10)
                .map(HistoricCandle::getClose)
                .flatMapToDouble(candle -> DoubleStream.of(candle.getUnits()))
                .toArray();
        double[] shortForecast1 = predictionService.predictWithAdaptiveModel(shortTermPrices, 3);
        log.info("Предсказание на 3 вперед {}", shortForecast1);
        double[] shortForecast2 = predictionService.predictWithAdaptiveModel(shortTermPrices, 5);
        log.info("Предсказание на 5 вперед {}", shortForecast2);
    }

}
