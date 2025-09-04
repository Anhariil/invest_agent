package ru.invest_agent.demo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.core.InvestApi;
import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.core.models.Money;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
//@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class PrivateTest {

    private static InvestApi investApi;
    private static String accountId;

    //создаем тестовый аккаунт для проверки методов
    @BeforeAll
    public static void createTestAccount(){
        Environment env = SpringApplication.run(InvestAgentApplication.class).getEnvironment();
        String sandBoxToken = env.getProperty("invest_api.token.sand_box");
        investApi = InvestApi.createSandbox(Objects.requireNonNull(sandBoxToken));
        accountId = investApi.getSandboxService().openAccountSync();
        log.info("Открыт новый счет с id = {}", accountId);
        log.info("Всего открыто счетов {}", investApi.getSandboxService().getAccountsSync().size());
        assertEquals(1,investApi.getSandboxService().getAccountsSync().size());
    }

    //удаляем тестовый аккаунт для проверки методов
    @AfterAll
    public static void deleteTestAccount(){
        investApi.getSandboxService().closeAccount(accountId);
        assertEquals(0,investApi.getSandboxService().getAccountsSync().size());

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
        }
    }

}
