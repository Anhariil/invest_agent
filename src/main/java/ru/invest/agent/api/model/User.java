package ru.invest.agent.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.constant.UserStatus;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User
//        implements Serializable
{
    @Id
    private String id;
    private String token;
    private String phone;
    private String name;
    private String lastName;
    private String middleName;
    private Long telegramChatId;
    private List<InvestAccount> accounts;
    private String mainAccount;
    private String mainAccountName;
    @JsonIgnore //TODO json бесится с этих полей - поменять формат или использовать UserDTO
    private Instant createDateTime;
    private UserStatus userStatus = UserStatus.NEW;
    private TradeStatus tradeStatus = TradeStatus.SANDBOX;
    private AccountHistory accountHistory;

    public void setPortfolioData(String portfolioData, Instant portfolioDate) {
        if (getAccountHistory() == null){
            setAccountHistory(new AccountHistory());
        }
        getAccountHistory().setPortfolioLastUpdateDate(portfolioDate);
        getAccountHistory().setPortfolioData(portfolioData);
    }

    public void setAnalyticsData(String analyticsData, Instant analyticsDate) {
        if (getAccountHistory() == null){
            setAccountHistory(new AccountHistory());
        }
        getAccountHistory().setAnalyticsData(analyticsData);
        getAccountHistory().setAnalyticsLastUpdateDate(analyticsDate);
    }
}
