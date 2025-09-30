package ru.invest.agent.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.InvestAccount;
import ru.invest.agent.api.model.TwoValues;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@Slf4j
public class InvestApiService {

    // TODO ненужно? считать динамически?
    // минимальное соотношение деньги / стоимость портфеля, при котором возможно совершать операции
    private static final double MIN_MONEY_VALUE_FOR_BUYING = 0.1;
    // максимальное соотношение деньги / стоимость портфеля, при котором возможно совершать операции
    private static final double MAX_MONEY_VALUE_FOR_BUYING = 0.2;
    // минимальная разница цен при которой стоит идти в покупку / продажу
    // TODO надо прицениться
    private static final double MIN_VALUABLE_RATE = 1.005;
    // максимальное количество инструментов для обработки
    private static final int ASSETS_FROM_TOP_LIMITS = 20;
    private static final int MAX_ASSETS_TO_WORK = 5;

    //TODO настроить кэш? все равно данные раз в минуту обновляются
    private static List<Share> allAvailableShares; // TODO если многопоточность то = new CopyOnWriteArrayList<>() ?
    private static List<LastPrice> allAvailableSharesLastPrice;
    private final InvestApi investApi;
    private final AdvancedPredictionService advancedPredictionService;

    public InvestApiService(InvestApi investApi) {
        this.investApi = investApi;
        if (Objects.nonNull(investApi)) {
            setAllAvailableShares();
        }
        this.advancedPredictionService = new AdvancedPredictionService();
    }

    /**
     * Создание новых заявок на покупку / продажу активов или обновление текущих
     */
    public void createOrUpdateOrders(List<InvestAccount> accounts, TradeStatus tradeStatus) {
//        investApi.getOrdersService().getOrdersSync();
//        investApi.getOperationsService().getPortfolio()
        if (Objects.isNull(investApi)) {
            log.error("Соединение не установлено или возникла какая либо другая ошибка");
            throw new RuntimeException("Ошибка соединения"); // TODO ошибки моего приложения
        }
        for (InvestAccount account : accounts) {

            String accountId = account.getId();
            log.debug("Начинаем работать с аккаунтом '{}'", accountId);
            List<OrderState> orderStates = investApi.getOrdersService().getOrdersSync(accountId);

            // TODO лист цен?
//            var lastPrices = investApi.getMarketDataService().getLastPricesSync(orderStates.stream().map(OrderState::getFigi).toList());
            //TODO обновление текущих заявок
//            updateOrderStates(orderStates) // void или лист в ответ? // int с кол-вом заявок?

            int assetsOnWorkCount = 0;
            assetsOnWorkCount = investApi.getOperationsService().getPortfolioSync(accountId).getPositions().size(); //TODO проверить (надо исключать валюту)

            if (isAccountHasNoMoney(accountId)) {
                log.debug("На счету не достаточно средств для любых операций");
                break;
            }

            // не работаем с инструментами, на которые уже выставлены сделки
            String figiToExclude = orderStates.stream().map(OrderState::getFigi).reduce("", (a, b) -> a + b + ";")
                    // и с инструментами, которые уже присутствуют в портфеле см assetsOnWorkCount
                    + investApi.getOperationsService().getPortfolioSync(accountId).getPositions().stream().filter(p -> !p.getInstrumentType().equals("currency")).map(Position::getFigi).reduce("", (a, b) -> a + b + ";");

            //работаем только с акциями пока что
            List<Share> shares = getAllAvailableShares().stream()
                    .filter(s -> !figiToExclude.contains(s.getFigi()))
                    .filter(s -> s.getCurrency().equals("rub")) // только активы в рублях
                    .toList();

            List<LastPrice> sharesLastPrice = getSharesLastPrice(investApi, shares.stream().map(Share::getFigi).reduce("", (a, b) -> a + b + ";"));
            // преобразуем в Map
            Map<String, LastPrice> sharesLastPriceMap = sharesLastPrice.stream().collect(Collectors.toMap(LastPrice::getFigi, Function.identity()));
            List<String> figs = sharesLastPrice.stream().limit(ASSETS_FROM_TOP_LIMITS).map(LastPrice::getFigi).toList();
            for (String figi : figs) {
                log.debug("Начинаем работать с инструментом: '{}'", figi);
                LastPrice lastPrice = sharesLastPriceMap.get(figi);
                Share share = shares.stream().filter(s -> s.getFigi().equals(figi)).findFirst().orElseThrow();
                int lot = share.getLot();

                var minPrice = doubleToQuotation(quotationToDouble(lastPrice.getPrice()) * lot);
                if (isAccountHasMoneyToBuy(investApi, accountId, minPrice)) {
                    long quantity = getQuantity(investApi, accountId, minPrice);

                    double[] lastFigiPrices = getHistoricCandleList(investApi, figi).stream()
                            .limit(60).map(HistoricCandle::getClose)
                            .map(InvestApiService::quotationToDouble)
                            // разворачиваем обратно чтобы значения были от -60 до 0 по шкале времени
                            .toList().reversed().stream()
                            .flatMapToDouble(DoubleStream::of)
                            .toArray();
                    if (lastFigiPrices.length < 60) {
                        log.warn("По инструменту недостаточно данных для работы,тикет: '{}', получено свечей: '{}'",
                                share.getTicker(), lastFigiPrices.length);
                        break; // должны выйти из if
                    }
                    double[] predictPrices = advancedPredictionService.predictWithAdaptiveModel(lastFigiPrices, 10);

                    // смотреть макс предсказанное значение? фиксировать тренд?
                    TwoValues<Integer, Double>[] trends = new TwoValues[]{advancedPredictionService.getRiseTrendingValues(predictPrices),
                            advancedPredictionService.getFallTrendingValues(predictPrices)};

                    //растущий тренд - покупаем
//                    if (trends[0].getFirstValue() > trends[1].getFirstValue()){ //TODO ??
                    if (trends[0].getSecondValue() / lastFigiPrices[lastFigiPrices.length - 1] > MIN_VALUABLE_RATE) {
                        log.debug("Выставляем заявку на покупку");
                        //Выставляем заявку на покупку
                        createBestPriceOrder(investApi, accountId, figi, quantity, OrderDirection.ORDER_DIRECTION_BUY);
                        //Выставляем заявку на продажу по лимитной цене //TODO перенести в работу с активами?
                        Quotation sellingPrice = doubleToQuotation(trends[0].getSecondValue());
                        //TODO что делать если прогноз пошел вниз? во время работы с открытыми заказами смотреть отклонение от цели?
                        createLimitOrder(investApi, accountId, figi, quantity, sellingPrice, OrderDirection.ORDER_DIRECTION_SELL);
                        assetsOnWorkCount++;
                    } else if (lastFigiPrices[lastFigiPrices.length - 1] / trends[1].getSecondValue() > MIN_VALUABLE_RATE) {
                        //TODO шортить можно только с маржиналкой, надо разбираться
                        log.debug("Выставляем заявку на продажу (Шутка. Пока ничего не делаем)");
//                        assetsOnWorkCount++;

                    } else {
                        log.debug("Не выставляем заявку по инструменту");
                    }

                } else {
                    log.debug("На счету не достаточно денег для дальнейшей обработки, assetsOnWorkCount:'{}'", assetsOnWorkCount);
                    break; // должны выйти из цикла по figi
                }

                //конец?
                log.debug("Заканчиваем работу с инструментом: '{}'", figi);

                if (assetsOnWorkCount >= MAX_ASSETS_TO_WORK) {
                    log.debug("Достигли максимального кол-ва инструментов для обработки");
                    break;// должны выйти из цикла по figi
                }
            }
//            var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
//            var price = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
//                    .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();
//
//
            break; //TODO пока делаем только для одного аккаунта
        }
    }

    public static boolean isAccountHasNoMoney(InvestApi investApi, String accountId) {
        return !isAccountHasMoney(investApi, accountId);
    }

    public boolean isAccountHasNoMoney(String accountId) {
        return !isAccountHasMoney(investApi, accountId);
    }

    /**
     * Считаем что на счету достаточно средств, если нашли валюту РФ и отношение {@code валюта / стоимость портфеля}
     * больше чем {@code MIN_MONEY_VALUE_FOR_BUYING}
     *
     */
    public static boolean isAccountHasMoney(InvestApi investApi, String accountId) {
        List<Money> monies = getAccountsMoneyList(investApi, accountId);
        Money totalAmount = getAccountsTotalAmount(investApi, accountId);

        // пока работаем только с рублями
        double rubles = monies.stream().filter(money -> money.getCurrency().equals("rub")).findFirst().map(Money::getValue).map(BigDecimal::doubleValue).orElse(0.0);
        return !(rubles < 10.0) && !(totalAmount.getValue().doubleValue() < 10.0)
                && !(rubles / totalAmount.getValue().doubleValue() < MIN_MONEY_VALUE_FOR_BUYING);
    }

    public boolean isAccountHasMoney(String accountId) {
        return isAccountHasMoney(investApi, accountId);
    }

    public static List<Money> getAccountsMoneyList(InvestApi investApi, String accountId) {
        return investApi.getOperationsService().getPositionsSync(accountId).getMoney();
    }

    public List<Money> getAccountsMoneyList(String accountId) {
        return getAccountsMoneyList(investApi, accountId);
    }

    public static Money getAccountsMoney(InvestApi investApi, String accountId, String currency) {
        return getAccountsMoneyList(investApi, accountId).stream()
                .filter(money -> money.getCurrency().equals(currency))
                .findFirst()
                .orElse(Money.builder().value(BigDecimal.valueOf(0)).build());
    }

    /**
     * Возвращает стоимость портфеля в рублях
     *
     */
    public static Money getAccountsTotalAmount(InvestApi investApi, String accountId) {
        return investApi.getOperationsService().getPortfolioSync(accountId).getTotalAmountPortfolio();
    }

    // по умолчанию работаем только с акциями
//    public static void createOrUpdateOrders(InvestApi investApi) {
//        createOrUpdateOrders(investApi, List.of(AssetsConstant.SHARE));
//    }
    public static List<Share> getAllAvailableShares(InvestApi investApi) {
        if (CollectionUtils.isEmpty(allAvailableShares)) {
            allAvailableShares = investApi.getInstrumentsService().getAllSharesSync().stream()
                    .filter(s -> !s.getForQualInvestorFlag()
                            && s.getApiTradeAvailableFlag()
                            && s.getBuyAvailableFlag()
                            && s.getSellAvailableFlag())
                    .toList();
        }
        return allAvailableShares;
    }

    private List<Share> getAllAvailableShares() {
        if (CollectionUtils.isEmpty(allAvailableShares)) {
            setAllAvailableShares();
        }
        return allAvailableShares;
    }

    private void setAllAvailableShares() {
        allAvailableShares = investApi.getInstrumentsService().getAllSharesSync().stream()
                .filter(s -> !s.getForQualInvestorFlag()
                        && s.getApiTradeAvailableFlag()
                        && s.getBuyAvailableFlag()
                        && s.getSellAvailableFlag())
                .toList();
    }

    /**
     * Возвращает отсортированный по цене инструмента список цен акций
     *
     */
    public static List<LastPrice> getSharesLastPrice(InvestApi investApi, String figiList) {
        if (CollectionUtils.isEmpty(allAvailableSharesLastPrice)) {
            List<String> allAvailableSharesFigi = getAllAvailableShares(investApi).stream().map(Share::getFigi).toList();
            allAvailableSharesLastPrice = investApi.getMarketDataService().getLastPricesSync(allAvailableSharesFigi);
        }
        //TODO вынести?
        Map<String, Integer> lotMap = getAllAvailableShares(investApi).stream()
                .filter(lastPrice -> figiList.contains(lastPrice.getFigi()))
                .collect(Collectors.toMap(Share::getFigi, Share::getLot));
        return allAvailableSharesLastPrice.stream()
                .filter(lastPrice -> figiList.contains(lastPrice.getFigi()))
                .sorted((lastPrice1, lastPrice2) -> {
                    double price1 = quotationToDouble(lastPrice1.getPrice()) * lotMap.get(lastPrice1.getFigi());
                    double price2 = quotationToDouble(lastPrice2.getPrice()) * lotMap.get(lastPrice2.getFigi());
                    return Double.compare(price1, price2);
                })
                .toList();
    }

    /**
     * Лист цен инструмента. Даты идут из настоящего в прошлое, время -3 от МСК
     *
     */
    public static List<HistoricCandle> getHistoricCandleList(InvestApi investApi, String figi, CandleInterval candleInterval, ChronoUnit chronoUnit, long amountToSubtract) {
        return investApi.getMarketDataService().getCandlesSync(figi, Instant.now().minus(amountToSubtract, chronoUnit),
                Instant.now(), candleInterval).reversed();
    }

    /**
     * По умолчанию получаем лист цен за последний день с шагом в 1 минуту. Даты идут из настоящего в прошлое, время
     * -3 от МСК
     */
    public static List<HistoricCandle> getHistoricCandleList(InvestApi investApi, String figi) {
        return getHistoricCandleList(investApi, figi, CandleInterval.CANDLE_INTERVAL_1_MIN, ChronoUnit.DAYS, 1);
    }

    private static boolean isAccountHasMoneyToBuy(InvestApi investApi, String accountId, Quotation price) {
        Money money = getAccountsMoney(investApi, accountId, "rub");
        return money.getValue().doubleValue() > 0
                && money.getValue().doubleValue() > quotationToDouble(price);
    }

    private static long getQuantity(InvestApi investApi, String accountId, Quotation price) {
        double money = getAccountsMoney(investApi, accountId, "rub").getValue().doubleValue();
        double priceInDouble = quotationToDouble(price);

        return BigDecimal.valueOf(money * MAX_MONEY_VALUE_FOR_BUYING)
                .divide(BigDecimal.valueOf(priceInDouble), 0, RoundingMode.DOWN).longValue();
    }

    //TODO вспомогательный метод, вынести бы отсюда
    public static double quotationToDouble(Quotation price) {
        double returnValue = price.getUnits();
        int number = price.getNano();

        if (number == 0) {
            return returnValue;
        }

        // Шаг 1: Определяем количество цифр
        int numDigits = (int) Math.log10(Math.abs(number)) + 1;
        // Шаг 2: Рассчитываем делитель
        double divisor = Math.pow(10, numDigits);
        // Шаг 3: Делим с сохранением знака
        return returnValue + (number / divisor);
    }

    //TODO вспомогательный метод, вынести бы отсюда
    public static Quotation doubleToQuotation(double price) {
//        log.debug("price: {}",price);
        long units = (long) price;
        int nano = 0;

        String str = Double.toString(price);
        int dotIndex = str.indexOf('.');
        int eIndex = str.indexOf('E');

        if (dotIndex != -1) {
            // Определяем длину дробной части
            int length = (eIndex == -1) ? str.length() - dotIndex - 1 : eIndex - dotIndex - 1;

            // Извлекаем и преобразуем
            String fractionalStr = str.substring(dotIndex + 1, eIndex == -1 ? str.length() : eIndex);
            if (length < 9 && !fractionalStr.equals("0")) { // длина nano должна быть 9 символов
                fractionalStr = fractionalStr + "0".repeat(9 - length);
            } else if (length > 9) {
                fractionalStr = fractionalStr.substring(0, 9);
            }
            nano = Integer.parseInt(fractionalStr);
        }

        return Quotation.newBuilder().setUnits(units).setNano(nano).build();
    }

    public static String createLimitOrder(InvestApi investApi, String accountId, String figi, long quantity, Quotation price, OrderDirection orderDirection) {
        //Выставляем заявку по лимитной цене
        var orderId = investApi.getOrdersService()
                .postOrderSync(figi, quantity, price, orderDirection, accountId, OrderType.ORDER_TYPE_LIMIT,
                        UUID.randomUUID().toString()).getOrderId();

        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        var order = investApi.getOrdersService().getOrdersSync(accountId).stream()
                .filter(el -> orderId.equals(el.getOrderId())).findFirst().orElse(null);
        if (Objects.nonNull(order)) {
            log.debug("заявка с id {} есть в списке активных заявок, параметры заявки -- инструмент: {}, цена: {}, " +
                    "количество: {}, направление заявки {}", orderId, figi, quotationToDouble(price), quantity, orderDirection);
        } else {
            log.warn("заявки с id {} нет в списке активных заявок!", orderId);
        }

        return orderId;
    }

    public static String createBestPriceOrder(InvestApi investApi, String accountId, String figi, long quantity, OrderDirection orderDirection) {
        //Выставляем заявку по лучшей цене
        var orderId = investApi.getOrdersService()
                .postOrderSync(figi, quantity, Quotation.newBuilder().build(), orderDirection, accountId, OrderType.ORDER_TYPE_BESTPRICE,
                        UUID.randomUUID().toString()).getOrderId();

        //TODO вытаскивать цену через логи или по инструментам и figi
        //Заявка по лучшей цене всегда исполняется сразу? а по рынку OrderType.ORDER_TYPE_MARKET ?
        log.debug("Заявка на покупку по лучшей цене исполнена. Параметры заявки -- инструмент: {}, цена: {}, " +
                "количество: {}, направление заявки {}", figi, "???" /*quotationToDouble(price)*/, quantity, orderDirection);

        return orderId;
    }
}
