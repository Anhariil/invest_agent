package ru.invest.agent.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.model.InvestAccount;
import ru.invest.agent.api.model.TwoValues;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final long ASSETS_FROM_TOP_LIMITS = 5;

    private static List<Share> allAvailableShares; // TODO если многопоточность то = new CopyOnWriteArrayList<>() ?
    private static List<LastPrice> allAvailableSharesLastPrice;
    private final InvestApi investApi;
    @Autowired
    private AdvancedPredictionService advancedPredictionService;

    public InvestApiService(InvestApi investApi) {
        this.investApi = investApi;
        setAllAvailableShares();
    }

    /**
     * Создание новых заявок на покупку / продажу активов или обновление текущих
     *
     *
     */
    public void createOrUpdateOrders(List<InvestAccount> accounts, TradeStatus tradeStatus) {
//        investApi.getOrdersService().getOrdersSync();
//        investApi.getOperationsService().getPortfolio()

        for (InvestAccount account : accounts) {

            String accountId = account.getId();
            log.debug("Начинаем работать с аккаунтом '{}'", accountId);
            List<OrderState> orderStates = investApi.getOrdersService().getOrdersSync(accountId);

            // TODO лист цен?
//            var lastPrices = investApi.getMarketDataService().getLastPricesSync(orderStates.stream().map(OrderState::getFigi).toList());
            //TODO обновление текущих заявок
//            updateOrderStates(orderStates) // void или лист в ответ?

            if (isAccountHasNoMoney(accountId)) {
                log.debug("На счету не достаточно средств для операций");
                break;
            }

            // не работаем с инструментами, на которые уже выставлены сделки
            String figiToExclude = orderStates.stream().map(OrderState::getFigi).reduce("", (a, b) -> a + b + ";");

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
                log.debug("Начинаем работать с инструментом '{}'", figi);
                LastPrice lastPrice = sharesLastPriceMap.get(figi);
                Share share = shares.stream().filter(s -> s.getFigi().equals(figi)).findFirst().orElseThrow();
                int lot = share.getLot();
                var minPrice = Quotation.newBuilder().setUnits(lastPrice.getPrice().getUnits() * lot)
                        .setNano(lastPrice.getPrice().getNano() * lot).build();
                if (isAccountHasMoneyToBuy(investApi, accountId, minPrice)) {
                    long quantity = getQuantity(investApi, accountId, minPrice);

                    double[] lastFigiPrices = getHistoricCandleList(investApi, figi).reversed().stream()
                            .limit(60).map(HistoricCandle::getClose)
                            .map(InvestApiService::quotationToDouble)
                            // разворачиваем обратно чтобы значения были от -60 до 0 по шкале времени
                            .toList().reversed().stream() // TODO проверить
                            .flatMapToDouble(DoubleStream::of)
                            .toArray();
                    double[] predictPrices = advancedPredictionService.predictWithAdaptiveModel(lastFigiPrices, 10);

                    // смотреть макс предсказанное значение? фиксировать тренд?
                    TwoValues<Integer, Double>[] trends = new TwoValues[]{advancedPredictionService.getRiseTrendingValues(predictPrices),
                            advancedPredictionService.getFallTrendingValues(predictPrices)};

                    //растущий тренд - покупаем
//                    if (trends[0].getFirstValue() > trends[1].getFirstValue()){ //TODO ??
                    if (trends[0].getSecondValue() / lastFigiPrices[lastFigiPrices.length - 1] > MIN_VALUABLE_RATE) {
                        //Выставляем заявку на покупку
                        createBestPriceOrder(investApi, accountId, figi, quantity, OrderDirection.ORDER_DIRECTION_BUY);
                        //Выставляем заявку на продажу по лимитной цене? //TODO проверить работает ли ок, перенести в работу с активами?
                        Quotation sellingPrice = doubleToQuotation(trends[0].getSecondValue());
                        createLimitOrder(investApi, accountId, figi, quantity, sellingPrice, OrderDirection.ORDER_DIRECTION_SELL);
                    } else if (lastFigiPrices[lastFigiPrices.length - 1] / trends[1].getSecondValue() > MIN_VALUABLE_RATE) {
                        //TODO шортить можно только с маржиналкой, надо разбираться
                        break;
                    }

                    //конец?
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

    private boolean isAccountHasNoMoney(String accountId) {
        return !isAccountHasMoney(investApi, accountId);
    }
    /**
     * Считаем что на счету достаточно средств, если нашли валюту РФ и отношение {@code валюта / стоимость портфеля}
     * больше чем {@code MIN_MONEY_VALUE_FOR_BUYING}
     *
     * @param investApi
     * @param accountId
     * @return
     */
    private static boolean isAccountHasMoney(InvestApi investApi, String accountId) {
        List<Money> monies = getAccountsMoneyList(investApi, accountId);
        Money totalAmount = getAccountsTotalAmount(investApi, accountId);

        // пока работаем только с рублями
        double rubles = monies.stream().filter(money -> money.getCurrency().equals("rub")).findFirst().map(Money::getValue).map(BigDecimal::doubleValue).orElse(0.0);
        return !(rubles < 10.0) && !(totalAmount.getValue().doubleValue() < 10.0)
                && !(rubles / totalAmount.getValue().doubleValue() < MIN_MONEY_VALUE_FOR_BUYING);
    }

    public static List<Money> getAccountsMoneyList(InvestApi investApi, String accountId) {
        return investApi.getOperationsService().getPositionsSync(accountId).getMoney();
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
     * @param investApi
     * @param accountId
     * @return
     */
    public static Money getAccountsTotalAmount(InvestApi investApi, String accountId) {
        return investApi.getOperationsService().getPortfolioSync(accountId).getTotalAmountPortfolio();
    }

    // по умолчанию работаем только с акциями
//    public static void createOrUpdateOrders(InvestApi investApi) {
//        createOrUpdateOrders(investApi, List.of(AssetsConstant.SHARE));
//    }
    private static List<Share> getAllAvailableShares(InvestApi investApi) {
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

    private void setAllAvailableShares(){
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
     * @param investApi
     * @param figiList  - список акций для сортировки
     * @return
     */
    private static List<LastPrice> getSharesLastPrice(InvestApi investApi, String figiList) {
        if (CollectionUtils.isEmpty(allAvailableSharesLastPrice)) {
            List<String> allAvailableSharesFigi = getAllAvailableShares(investApi).stream().map(Share::getFigi).toList();
            allAvailableSharesLastPrice = investApi.getMarketDataService().getLastPricesSync(allAvailableSharesFigi);
        }
        return allAvailableSharesLastPrice.stream()
                .filter(lastPrice -> figiList.contains(lastPrice.getFigi()))
                .sorted((lastPrice1, lastPrice2) -> // TODO сортируем по цене одного инструмента, а надо учитывать лоты
                        Math.toIntExact(lastPrice2.getPrice().getUnits() - lastPrice1.getPrice().getUnits())) //TODO не учитывается nano
                .toList();
    }

    public static List<HistoricCandle> getHistoricCandleList(InvestApi investApi, String figi, CandleInterval candleInterval, ChronoUnit chronoUnit, long amountToSubtract) {
        return investApi.getMarketDataService().getCandlesSync(figi, Instant.now().minus(amountToSubtract, chronoUnit),
                Instant.now(), candleInterval).reversed();
    }

    /**
     * По умолчанию получаем лист цен за последний день с шагом в 1 минуту
     *
     * @param investApi
     * @param figi
     * @return
     */
    public static List<HistoricCandle> getHistoricCandleList(InvestApi investApi, String figi) {
        return getHistoricCandleList(investApi, figi, CandleInterval.CANDLE_INTERVAL_1_MIN, ChronoUnit.DAYS, 1);
    }

    private static boolean isAccountHasMoneyToBuy(InvestApi investApi, String accountId, Quotation price) {
        Money money = getAccountsMoney(investApi, accountId, "rub");
        return money.getValue().doubleValue() > 0
                //TODO как нормально свести Money и Quotation
                && money.getValue().doubleValue() > price.getNano();
    }

    private static long getQuantity(InvestApi investApi, String accountId, Quotation price) {
        double money = getAccountsMoney(investApi, accountId, "rub").getValue().doubleValue();
        double priceInDouble = quotationToDouble(price);

        return BigDecimal.valueOf(money * MAX_MONEY_VALUE_FOR_BUYING)
                .divide(BigDecimal.valueOf(priceInDouble), 0, RoundingMode.DOWN).longValue();
    }

    //TODO вспомогательный метод, вынести бы отсюда
    //TODO проверить
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
    //TODO проверить 
    public static Quotation doubleToQuotation(double price) {
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
            nano = Integer.parseInt(fractionalStr);
        }

        return Quotation.newBuilder().setUnits(units).setNano(nano).build();
    }

    public static String createLimitOrder(InvestApi investApi, String accountId, String figi, long quantity, Quotation price, OrderDirection orderDirection) {
        //Выставляем заявку на покупку по лимитной цене
        var orderId = investApi.getOrdersService()
                .postOrderSync(figi, quantity, price, orderDirection, accountId, OrderType.ORDER_TYPE_LIMIT,
                        UUID.randomUUID().toString()).getOrderId();

        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        var orders = investApi.getOrdersService().getOrdersSync(accountId);
        if (orders.stream().anyMatch(el -> orderId.equals(el.getOrderId()))) {
            log.debug("заявка с id {} есть в списке активных заявок", orderId);
        }

        return orderId;
    }

    public static String createBestPriceOrder(InvestApi investApi, String accountId, String figi, long quantity, OrderDirection orderDirection) {
        //Выставляем заявку на покупку по лучшей цене
        //TODO проверить не падаем ли из-зи Quotation.newBuilder().build()
        var orderId = investApi.getOrdersService()
                .postOrderSync(figi, quantity, Quotation.newBuilder().build(), orderDirection, accountId, OrderType.ORDER_TYPE_BESTPRICE,
                        UUID.randomUUID().toString()).getOrderId();

        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        var orders = investApi.getOrdersService().getOrdersSync(accountId);
        if (orders.stream().anyMatch(el -> orderId.equals(el.getOrderId()))) {
            log.debug("заявка с id {} есть в списке активных заявок", orderId);
        }

        return orderId;
    }
}
