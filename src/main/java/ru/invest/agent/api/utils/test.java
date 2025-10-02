package ru.invest.agent.api.utils;

import org.springframework.beans.factory.annotation.Value;
import ru.tinkoff.piapi.core.InvestApi;


public class test {

    @Value("${invest_api.token.sand_box}")
    private  String sandBoxToken;

    public static void main() {
//        var sandboxApi = InvestApi.createSandbox(sandBoxToken);
//        System.out.println(sandboxApi.isSandboxMode());
    }
}
