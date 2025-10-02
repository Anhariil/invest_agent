package ru.invest.agent.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.invest.agent.api.constant.TradeStatus;
import ru.invest.agent.api.constant.UserStatus;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String token;
    private String phone;
    private String name;
    private String lastName;
    private String middleName;
    private Long telegramChatId;
    private List<InvestAccount> accounts;
    private Instant createDateTime;
    private UserStatus userStatus = UserStatus.NEW;
    private TradeStatus tradeStatus = TradeStatus.SANDBOX;
}
