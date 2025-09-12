//package ru.invest.agent.api.config;
//
//import me.paulschwarz.springdotenv.*;
//import org.springframework.context.ApplicationContextInitializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.core.env.ConfigurableEnvironment;
//import org.springframework.core.env.MutablePropertySources;
//
//
//@Configuration
//public class EnvConfig
//        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//
//    @Override
//    public void initialize(ConfigurableApplicationContext context) {
//        // 1. Определяем активный профиль
//        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
//        String activeProfile = activeProfiles.length > 0 ? activeProfiles[0] : "default";
//
//        // 2. Загружаем соответствующий .env файл
//        String envFile = ".env." + activeProfile;
//        DotenvConfig dotenv = new DotenvConfig(new) .builder()
//                .filename(".env." + activeProfile)  // Имя файла
//                .ignoreIfMissing(true)              // Игнорировать если файл отсутствует
//                .systemProperties(false)            // Не устанавливать в System Properties
//                .build();
//
//        // 3. Создаем PropertySource
//        DotenvPropertySource propertySource = new DotenvPropertySource(dotenv);
//
//        // 4. Добавляем в Environment
//        ConfigurableEnvironment env = context.getEnvironment();
//        MutablePropertySources propertySources = env.getPropertySources();
//        propertySources.addFirst(propertySource);
//    }
//}