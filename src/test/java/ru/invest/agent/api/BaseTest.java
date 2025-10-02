package ru.invest.agent.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.invest.agent.InvestAgentApplication;
import ru.invest.agent.api.repository.UserRepository;

@SpringBootTest(
        classes = InvestAgentApplication.class,
        properties = {
                "spring.data.mongodb.uri=", // Явно очищаем URI
                "spring.autoconfigure.exclude=" + // Отключаем монго для тестов
                        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
        }
)
@ActiveProfiles("test")
public abstract class BaseTest {

    @Value("${file.path}")
    public String testPath;

    @MockitoBean
    private UserRepository userRepository; // mock чтобы при запуске приложения не падали по созданию подключения к монго
}
