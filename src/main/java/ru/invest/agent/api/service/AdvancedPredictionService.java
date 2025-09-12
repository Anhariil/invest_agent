package ru.invest.agent.api.service;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AdvancedPredictionService {

    // Основной метод прогнозирования
    public double[] predictWithAdaptiveModel(double[] prices, int forecastSteps) {
        final double[] trends = polynomialRegression(prices, 3, forecastSteps);
        final double[] smoothed = exponentialSmoothing(prices, 0.7 , forecastSteps);

        // Рассчитываем волатильность для адаптивного веса
        final double volatility = calculateVolatility(prices);
        final double alpha = 1 / (1 + Math.exp(-0.5 * (volatility - 0.02)));

        // Комбинируем прогнозы
        final double[] combined = new double[forecastSteps];
        for (int i = 0; i < forecastSteps; i++) {
            combined[i] = alpha * trends[i] + (1 - alpha) * smoothed[i];
        }

        return combined;
    }

    // Полиномиальная регрессия n-й степени
    private double[] polynomialRegression(double[] prices, int degree, int forecastSteps) {
        final WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i = 0; i < prices.length; i++) {
            points.add(i, prices[i]);
        }

        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        final double[] coefficients = fitter.fit(points.toList());

        // Прогнозируем будущие значения
        final double[] forecasts = new double[forecastSteps];
        for (int i = 0; i < forecastSteps; i++) {
            double prediction = 0;
            for (int j = 0; j <= degree; j++) {
                prediction += coefficients[j] * Math.pow(prices.length + i, j);
            }
            forecasts[i] = prediction;
        }

        return forecasts;
    }

    // Адаптивное экспоненциальное сглаживание
    private double[] exponentialSmoothing(double[] prices, double baseGamma, int forecastSteps) {
        if (prices.length < forecastSteps-1 ) return Arrays.copyOf(prices, forecastSteps);

        double[] forecasts = new double[forecastSteps];
        double smoothed = prices[0];

        // Сглаживание исторических данных с адаптивным коэффициентом
        for (int i = 1; i < prices.length; i++) {
            double volatility = Math.abs(prices[i] - prices[i - 1]) / prices[i - 1];
            double adaptiveGamma = baseGamma * (1 + 2 * volatility); // Адаптация к волатильности
            adaptiveGamma = Math.min(0.95, Math.max(0.1, adaptiveGamma)); // Ограничение 0.1-0.95

            smoothed = adaptiveGamma * prices[i] + (1 - adaptiveGamma) * smoothed;
        }

        // Прогнозирование (для будущих точек используем последнее сглаженное значение)
        Arrays.fill(forecasts, smoothed);
        return forecasts;
    }

    // Расчет волатильности (стандартное отклонение доходностей)
    private double calculateVolatility(double[] prices) {
        if (prices.length < 2) return 0.01;

        final double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = (prices[i] - prices[i - 1]) / prices[i - 1];
        }

        return new StandardDeviation().evaluate(returns);
    }
}