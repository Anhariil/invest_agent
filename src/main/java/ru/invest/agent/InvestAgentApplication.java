package ru.invest.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@Slf4j
public class InvestAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestAgentApplication.class, args);
        log.info("Запуск приложения успешен");
    }

}
