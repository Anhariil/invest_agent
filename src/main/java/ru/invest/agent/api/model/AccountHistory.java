package ru.invest.agent.api.model;

import lombok.Data;

import java.time.Instant;

@Data
public class AccountHistory {

    private Instant portfolioLastUpdateDate;
    private String portfolioData;
    private Instant analyticsLastUpdateDate;
    private String  analyticsData;
}
