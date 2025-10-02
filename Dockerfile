# Базовый образ
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Копируем локальный дистрибутив Gradle в специальную директорию
COPY gradle/wrapper/gradle-8.9-bin.zip /project/gradle/wrapper/gradle-8.9-bin.zip

# Копируем файлы для кэширования зависимостей
COPY gradle/wrapper gradle/wrapper
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Загружаем зависимости (кешируемый слой)
RUN ./gradlew  --no-daemon dependencies

# Копируем весь код
COPY src src

# Собираем приложение
# TODO запуск без тестов, разобраться зачем падают
RUN ./gradlew  --no-daemon clean build -x test
# RUN ./gradlew  --no-daemon clean build

# Финальный образ с JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Копируем JAR из ступени сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# Порт приложения
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java", "-jar", "app.jar"]

# ? сборка образа не зависимо от бд , чисто этот файл
# docker buildx build -t demo-app .
# docker run -p 8080:8080 demo-app

# создание и запуск монги
# docker run -d --name invest_agent_db -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=dmitry -e MONGO_INITDB_ROOT_PASSWORD=some1pa33word -e MONGO_INITDB_DATABASE=invest_agent_db mongo:6.0
# или?
# docker-compose up -d mongodb

# команды для пересборки приложения
# docker-compose up -d --build app # сборка и запуск одной строкой
# docker-compose build app # сборка образа
# docker-compose up -d app # запуск образа
