package ru.invest.agent.telegram.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChartGenerator {

    public static byte[] generateSimpleBarChart(String title, String[] columnNames, double[] columnValues,
                                                String xAxisLabel, String yAxisLabel) throws IOException {

        if (columnNames.length != columnValues.length) {
            throw new RuntimeException("Длины массивов columnNames и columnValues не равны");
        }

        // Создаем набор данных
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < columnValues.length; i++) {
            dataset.addValue(columnValues[i], "Активы", columnNames[i]); //??
        }

        // Создаем график
        JFreeChart chart = ChartFactory.createBarChart(title, xAxisLabel, yAxisLabel, dataset,
                PlotOrientation.VERTICAL,false,false,false);

        // Настраиваем размер
        BufferedImage image = chart.createBufferedImage(800, 600);

        // Конвертируем в byte[]
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeBufferedImageAsPNG(baos, image);
            return baos.toByteArray();
        }
    }

    public static byte[] generateSimpleLineChart(String title, String[] columnNames, double[] columnValues,
                                                String xAxisLabel, String yAxisLabel) throws IOException {

        if (columnNames.length != columnValues.length) {
            throw new RuntimeException("Длины массивов columnNames и columnValues не равны");
        }

        // Создаем набор данных
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < columnValues.length; i++) {
            dataset.addValue(columnValues[i], "Активы", columnNames[i]); //??
        }

        // Создаем график
        JFreeChart chart = ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel, dataset,
                PlotOrientation.VERTICAL,false,false,false);

        // Настраиваем размер
        BufferedImage image = chart.createBufferedImage(800, 600);

        // Конвертируем в byte[]
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeBufferedImageAsPNG(baos, image);
            return baos.toByteArray();
        }
    }

    public static byte[] generateSimplePieChart(String title, String[] columnNames, double[] columnValues) throws IOException {

        if (columnNames.length != columnValues.length) {
            throw new RuntimeException("Длины массивов columnNames и columnValues не равны");
        }

        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();

        for (int i = 0; i < columnValues.length; i++) {
            pieDataset.setValue(columnNames[i], columnValues[i]);
        }
        // Создаем график
        JFreeChart chart = ChartFactory.createPieChart(title, pieDataset,false,false,false);

        // Настраиваем размер
        BufferedImage image = chart.createBufferedImage(800, 600);

        // Конвертируем в byte[]
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeBufferedImageAsPNG(baos, image);
            return baos.toByteArray();
        }
    }
}