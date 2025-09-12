package ru.invest.agent.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class ChartController {


    @GetMapping("/chart")
    public String showChart(Model model) throws JsonProcessingException {
        // Пример данных для диаграммы
        String[] labels = {"Категория A", "Категория B", "Категория C"};
        int[] values = {30, 50, 20};
        String[] colors = {"#FF6384", "#36A2EB", "#FFCE56"};

        ObjectMapper mapper = new ObjectMapper(); // оборачиваем объект для чтения js в jsp
        model.addAttribute("labels", mapper.writeValueAsString(labels));
        model.addAttribute("values", mapper.writeValueAsString(values));
        model.addAttribute("colors", mapper.writeValueAsString(colors));

        return "testChart"; // Имя JSP-файла
    }

    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        return "pong";
    }


}