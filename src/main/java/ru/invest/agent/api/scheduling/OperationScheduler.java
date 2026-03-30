package ru.invest.agent.api.scheduling;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

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
    // в выходные - с 10 до 19 (кроме праздников)
    //TODO вынести проверку работоспособности биржи перед запуском шедуллера?
//    @Scheduled(cron = "0 * 7-22 * * MON-FRI")
    @Scheduled(fixedDelay = 120_000) // для теста ставим так (2 мин)
    public void analysisMarket() throws InterruptedException {
        log.info("Начало работы analysisMarket");
//        List<String> tokens = new ArrayList<>(users.size());
        for (User user : users) {
            log.debug("Обработка клиента: '{}'", user.getName());
            String token = user.getToken();
            TradeStatus tradeStatus = user.getTradeStatus();
//            tokens.add(token);
            InvestApi investApi = InvestApiUtils.createInvestApi(token, tradeStatus);

            log.debug("Успешно подключились");

            List<InvestAccount> accounts = user.getAccounts();
            String mainAccount = user.getMainAccount();

            // TODO находить аккаунты, передавать параметры для работы, если аккаунт не активин - уведомления юзеру?
            // TODO передавать List<InvestAccount> ?
            if (CollectionUtils.isEmpty(accounts)
                    || accounts.stream().filter(a -> a.getId().equals(mainAccount)).findAny().isEmpty()) {
                //грузим аккаунты из тинь и заполняем в бд
                log.debug("Не нашли счетов в бд - смотрим в Т");

                accounts = new ArrayList<>();
                var investAccounts = InvestApiUtils.getAccountsList(investApi);
                for (Account account : investAccounts) {
                    accounts.add(new InvestAccount().createFromTinkoff(account));
                }
                user.setAccounts(accounts);

                if (StringUtils.isBlank(mainAccount)
                        || accounts.stream().filter(a -> a.getId().equals(mainAccount)).findAny().isEmpty()){
                    user.setMainAccount(accounts.getFirst().getId());
                }

                log.debug("Добавили '{}' счет(ов) для работы", accounts.size());

                userService.save(user);
                log.debug("Обновили счета для клиента: '{}'", user.getName());
            }

            //TODO хуяк и в продакшен, оставляем только главный сейчас
            accounts = accounts.stream().filter(a -> a.getId().equals(mainAccount)).toList();

            InvestApiService investApiService = new InvestApiService(investApi);
            try {
                // Просмотр и обновление текущих заявок (отмена заявки, пересмотр цены продажи/покупки с учетом нового анализа)
                investApiService.createOrUpdateOrders(accounts, tradeStatus);
            } catch (ApiRuntimeException e) {
                log.error("Превышен лимит запросов в минуту для '{}'", tradeStatus);
                if (tradeStatus == TradeStatus.SANDBOX) {
                    //Для ребута песочницы ждем 1 минут //TODO костыльно, проверить
                    log.info("Ожидаем 1 минуту перед новым подключением к песочнице");
                    TimeUnit.MINUTES.sleep(1);
                }
            } finally {
                //Закрытие подключения к апи через 1 секунду
                InvestApiUtils.destroy(token, 1);
                log.debug("Ожидаем 1 секунду перед новым подключением");
                TimeUnit.SECONDS.sleep(1);
            }
            log.debug("Завершение работы с клиентом: '{}'", user.getName());
        }
        log.info("Успешное завершение работы analysisMarket");
    }

    //раз в час 3600_000 обновляем пользователей (переделать через кэш?)
    @Scheduled(fixedDelay = 60_000)
    public void updateUsersList() {
        log.debug("Начало работы updateUsersList");

//        List<User> freshUsers = userService.findAllActive();
        List<User> freshUsers = userService.findAll(); //TODO поставить корректный метод после смены статуса юзверя
        users.clear();
        users.addAll(freshUsers);

        log.debug("Успешное завершение работы updateUsersList, новое кол-во активных пользователей: '{}'", users.size());

    }

}
