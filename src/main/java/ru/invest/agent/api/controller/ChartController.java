package ru.invest.agent.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.invest.agent.api.model.TwoValues;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@Controller
@Slf4j
public class ChartController {

    @GetMapping("/chart2")
    public String showChart2(Model model) {
        model.addAttribute("labels", List.of("Категория A", "Категория B", "Категория C"));
        model.addAttribute("values", List.of(30, 50, 20));
        model.addAttribute("colors", List.of("#FF6384", "#36A2EB", "#FFCE56"));
        return "chart2"; // Имя файла без расширения
    }

    @GetMapping("/chart")
    public String showChart(Model model) {
        model.addAttribute("labels", List.of("Категория A", "Категория B", "Категория C"));
        model.addAttribute("values", List.of(30, 50, 20));
        model.addAttribute("colors", List.of("#FF6384", "#36A2EB", "#FFCE56"));
        return "chart"; // Имя файла без расширения
    }

    @GetMapping("/multi-scatter")
    public String showMultiScatter(Model model) {
        // Первый набор точек
        double[] x1 = {1, 2, 3, 4, 5};
        double[] y1 = {2, 4, 6, 8, 10};

        // Второй набор точек
        double[] x2 = {1, 2, 3, 4, 5};
        double[] y2 = {1, 3, 5, 7, 9};

        model.addAttribute("dataset1", Map.of(
                "xValues", Arrays.toString(x1),
                "yValues", Arrays.toString(y1),
                "color", "rgba(54, 162, 235, 0.7)"
        ));

        model.addAttribute("dataset2", Map.of(
                "xValues", Arrays.toString(x2),
                "yValues", Arrays.toString(y2),
                "color", "rgba(255, 206, 86, 0.7)"
        ));

        return "multi-scatter";
    }

    @GetMapping("/multi-line-test")
    public String showMultiLineChartTest(Model model) {
        // Первая линия
        double[] x1 = {1, 2, 3, 4, 5};
        double[] y1 = {2, 4, 6, 4, 8};

        // Вторая линия
        double[] x2 = {1, 2, 3, 4, 5};
        double[] y2 = {1, 3, 5, 3, 1};

        model.addAttribute("line1", Arrays.toString(
                IntStream.range(0, x1.length)
                        .mapToObj(i -> Map.of("x", x1[i], "y", y1[i]))
                        .toArray()
        ));

        model.addAttribute("line2", Arrays.toString(
                IntStream.range(0, x2.length)
                        .mapToObj(i -> Map.of("x", x2[i], "y", y2[i]))
                        .toArray()
        ));

        return "multi-line";
    }

    @PostMapping("/multi-line")
    public String showMultiLineChart(Model model,
                                     @RequestBody TwoValues<double[],double[]> values) {

        double[] xLine = new double[values.getFirstValue().length];
        for (int i = 0; i < xLine.length; i++) {
            xLine[i] = i;
        }
        // Первая линия
        double[] y1 = values.getFirstValue();

        // Вторая линия
        double[] y2 = values.getFirstValue();

        model.addAttribute("line1", Arrays.toString(
                IntStream.range(0, xLine.length)
                        .mapToObj(i -> Map.of("x", xLine[i], "y", y1[i]))
                        .toArray()
        ));

        model.addAttribute("line2", Arrays.toString(
                IntStream.range(0, xLine.length)
                        .mapToObj(i -> Map.of("x", xLine[i], "y", y2[i]))
                        .toArray()
        ));

        return "multi-line";
    }

    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        return "pong";
    }


}