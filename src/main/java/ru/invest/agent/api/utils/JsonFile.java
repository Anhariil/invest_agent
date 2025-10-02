package ru.invest.agent.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class JsonFile {
    public static boolean saveJson(String json, String filePath) {
        try {
            Path path = Path.of(filePath);

            if (Files.notExists(path.getParent())) {
                log.error("Ошибка сохранения JSON, некорректный путь: '{}'",
                        StringUtils.substringBeforeLast(filePath, "/"));
                // Создать директории, если их нет
//                Files.createDirectories(path.getParent());
            }

            if (!filePath.endsWith(".json")) {
                filePath += ".json";
            }

            // Записать с опциями: CREATE, TRUNCATE_EXISTING, WRITE
            Files.writeString(
                    path,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            log.info("Успешная запись JSON файла '{}' в директорию '{}'"
                    , StringUtils.substringAfterLast(filePath, "/")
                    , StringUtils.substringBeforeLast(filePath, "/"));
            return true;
        } catch (IOException e) {
            log.error("Ошибка сохранения JSON: '{}'", e.getMessage());
            return false;
        }
    }

    public static String readJson(String filePath) {
        String result;

        try {
            if (!filePath.endsWith(".json")) {
                log.error("Указанный документ не JSON: '{}'", StringUtils.substringAfterLast(filePath, "/"));
                return null;
            }

            Path path = Path.of(filePath);
            if (Files.notExists(path.getParent())) {
                log.error("Ошибка загрузки JSON, некорректный путь: '{}'",
                        StringUtils.substringBeforeLast(filePath, "/"));
                return null;
            }
            result = Files.readString(path);
        } catch (IOException e) {
            log.error("Ошибка загрузки JSON: '{}'", e.getMessage());
            return null;
        }

        log.info("Успешная загрузка JSON файла '{}'"
                , StringUtils.substringAfterLast(filePath, "/"));
        return result;
    }
}
