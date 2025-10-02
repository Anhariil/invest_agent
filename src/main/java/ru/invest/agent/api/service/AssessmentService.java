package ru.invest.agent.api.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@Getter
public class AssessmentService { //TODO компонент

    //Минимальная глубина массива предсказания (не меньше 1)
    private static final int n = 2;

    private int bestIntervalLength;
    private double averageError;
    private double averageErrorPercent;
    private double averageErrorAccuracy;
    private double maxError;

    //TODO не double а BigDecimal
    //TODO не массив а лист
    public AssessmentService(double[] expected, double[] result) {
        if (!isIncomeMassiveValid(expected, result)) {
            return;
        }
        bestIntervalLength = getBestIntervalLength(expected, result);
        averageError = calculateFullAverageError(expected, result);
        averageErrorPercent = calculateFullAveragePercentError(expected, result);
        averageErrorAccuracy = calculateFullAveragePercentAccuracy(expected, result);
        maxError = calculateMaxPercentError(expected, result);
    }

    /**
     * Возвращает -1 если входящие параметры невалидны, иначе возвращает длину result при которой
     * достигается наибольшая точность предсказания
     *
     * @param expected массив реальных значений
     * @param result   массив предсказанных значений
     * @return -1 в случае ошибки, иначе индекс от {@code AssessmentService.n} до {@code expected.length}
     */
    public static int getBestIntervalLength(double[] expected, double[] result) {
        if (expected == null || expected.length <= n || result == null || result.length <= n) {
            log.error("Параметры сервиса пустые или их длины меньше n '{}': '{}', '{}' ", n, expected, result);
            return -1;
        }

        if (expected.length > result.length) {
            expected = Arrays.copyOfRange(expected, expected.length - result.length, expected.length);
        } else if (expected.length < result.length) {
            result = Arrays.copyOfRange(result, result.length - expected.length, result.length);
        }
        int maxLength = expected.length;

        int bestIntervalLength = maxLength;
        double max = calculateMaxPercentAccuracy(expected, result);
        double tempMax;
        for (int i = n; i < maxLength; i++) {
            tempMax = calculateMaxPercentAccuracy(Arrays.copyOfRange(expected, 0, i)
                    , Arrays.copyOfRange(result, 0, i));
            if (tempMax > 0 && tempMax >= max) {
                bestIntervalLength = i;
                max = tempMax;
            }
        }
        return bestIntervalLength;
    }

    /**
     * Рассчитывает среднее расхождение данных между двумя массивами <br/>
     * Результат может быть -1 в случае некорректных входных параметров, иначе ограничен снизу нулем
     * и не ограничен сверху
     *
     * @param expected массив реальных значений
     * @param result   массив предсказанных значений
     * @return -1 в случае ошибки, иначе значение больше нуля, показывающее среднюю погрешность измерений
     */
    //TODO найти норм имя а то чет э
    public static double calculateFullAverageError(double[] expected, double[] result) {
        if (BooleanUtils.isNotTrue(isIncomeMassiveValid(expected, result))) {
            return -1;
        }
        double resultError = 0;

        for (int i = 0; i < expected.length; i++) {
            resultError += Math.pow(Math.abs(expected[i] - result[i]), 2);
        }
        resultError = Math.sqrt(resultError / expected.length);
        log.debug("Текущая средняя погрешность предсказания для n = '{}': '{}'", expected.length, resultError);
        return resultError;
    }

    public static double calculateFullAveragePercentError(double[] expected, double[] result) {
        if (BooleanUtils.isNotTrue(isIncomeMassiveValid(expected, result))) {
            return -1;
        }

        double resultError = 0;

        for (int i = 0; i < expected.length; i++) {
            resultError += Math.abs(expected[i] - result[i]) / expected[i];
        }
        resultError = resultError / expected.length * 100;
        log.debug("Текущая средняя погрешность предсказания для n = '{}': '{}'%", expected.length, resultError);
        return resultError;
    }

    public static double calculateFullAveragePercentAccuracy(double[] expected, double[] result) {
        double resultError = calculateFullAveragePercentError(expected, result);
        if (resultError < 0) {
            return resultError;
        }
        double resultAccuracy = Math.abs(100 - resultError);
        log.debug("Текущая средняя точность предсказания для n = '{}': '{}'%", expected.length, resultAccuracy);
        return resultAccuracy;
    }

    private static boolean isIncomeMassiveValid(double[] expected, double[] result) {
        if (expected == null || result == null) {
            log.error("Один из параметров null");
            return false;
        }
        if (expected.length != result.length || expected.length == 0) {
            log.error("Длины массивов не равны или массивы пустые: '{}', '{}'", expected, result);
            return false;
        }
        return true;
    }

    public static double calculateMaxPercentError(double[] expected, double[] result) {
        if (BooleanUtils.isNotTrue(isIncomeMassiveValid(expected, result))) {
            return -1;
        }

        double resultError = 0;

        for (int i = 0; i < expected.length; i++) {
            double tempError = Math.abs(expected[i] - result[i]) / expected[i];
            resultError = Math.max(resultError, tempError);
        }
        log.debug("Текущая максимальная погрешность предсказания для n = '{}': '{}'%", expected.length, resultError);
        return resultError;
    }

    public static double calculateMaxPercentAccuracy(double[] expected, double[] result) {
        double resultError = calculateMaxPercentError(expected, result);
        if (resultError < 0) {
            return resultError;
        }
        double resultAccuracy = Math.abs(100 - resultError);
        log.debug("Текущая максимальная точность предсказания для n = '{}': '{}'%", expected.length, resultAccuracy);
        return resultAccuracy;
    }
}
