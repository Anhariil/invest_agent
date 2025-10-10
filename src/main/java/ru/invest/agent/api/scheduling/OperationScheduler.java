package ru.invest.agent.api.scheduling;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.InvestAccount;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.InvestApiService;
import ru.invest.agent.api.service.UserService;
import ru.invest.agent.api.utils.InvestApiUtils;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "invest_api.scheduler.operation", name = "enabled", havingValue = "true")
public class OperationScheduler {

    @Autowired
    private UserService userService;
    @Getter
    private final List<User> users = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        updateUsersList(); // Первоначальная загрузка после запуска приложения
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        log.info("Запуск OperationScheduler");
    }

    //время работы биржи в будни - 6.50/23.50 - надо проверить что все акции так и побольше узнать
    @Scheduled(cron = "0 * 7-21 * * MON-FRI")
//    @Scheduled(fixedDelay = 1) // для теста ставим так
    public void analysisMarket() {
        log.info("Начало работы analysisMarket");
//        List<String> tokens = new ArrayList<>(users.size());
        for (User user : users) {
            log.debug("Обработка клиента: '{}'", user.getName());
            String token = user.getToken();
            TradeStatus tradeStatus = user.getTradeStatus();
//            tokens.add(token);
            InvestApi investApi = InvestApiUtils.createInvestApi(token,tradeStatus);

            if (Objects.nonNull(investApi)) {
                log.debug("Успешно подключились");
                // TODO находить аккаунты, передавать параметры для работы, если аккаунт не активин - уведомления юзеру?
                // TODO передавать List<InvestAccount> ?
                List<InvestAccount> accounts = user.getAccounts();
                if (CollectionUtils.isEmpty(accounts)){
                    //грузим аккаунты из тинь и заполняем в бд
                    log.debug("Не нашли счетов в бд - смотрим в Т");

                    accounts = new ArrayList<>();
                    var investAccounts = investApi.getUserService().getAccountsSync();
                    for (Account account : investAccounts){
                        accounts.add(new InvestAccount().createFromTinkoff(account));
                    }
                    user.setAccounts(accounts);
                    log.debug("Добавили '{}' счет(ов) для работы", accounts.size());

                    userService.save(user);
                    log.debug("Обновили счета для клиента: '{}'", user.getName());
                }

                InvestApiService investApiService = new InvestApiService(investApi);
                // Просмотр и обновление текущих заявок (отмена заявки, пересмотр цены продажи/покупки с учетом нового анализа)
                investApiService.createOrUpdateOrders(accounts, tradeStatus);
            }
            log.debug("Завершение работы с клиентом: '{}'", user.getName());
        }
        log.info("Успешное завершение работы analysisMarket");
    }

    //раз в час обновляем пользователей (переделать через кэш?)
    @Scheduled(fixedDelay = 3600_000)
    public void updateUsersList() {
        log.debug("Начало работы updateUsersList");

//        List<User> freshUsers = userService.findAllActive();
        List<User> freshUsers = userService.findAll(); //TODO поставить корректный метод после смены статуса юзверя
        users.clear();
        users.addAll(freshUsers);

        log.debug("Успешное завершение работы updateUsersList, новое кол-во активных пользователей: '{}'", users.size());

    }

}
