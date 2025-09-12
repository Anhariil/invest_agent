package ru.invest.agent.api.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonConverter {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Создание объекта известного класса из json
     * <p>Пример вызова:
     * <pre>{@code
     * List<MyClass> users = JsonConverter.fromJson(jsonArray, new TypeReference<List<MyClass>>() {});
     * }</pre>
     * @param json строка представляющая объект нужного типа
     * @param typeReference представление типа, объект которого необходимо получить
     * @return объект представленного типа или {@code null}
     */
    public static <T> T convertJsonToObject(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("Ошибка конвертации JSON в объект типа: '{}', '{}' ", typeReference.getType().getTypeName(), e.getMessage());
            return null;
        }
    }

    public static <T> T convertJsonToObject(String json ) {
        return convertJsonToObject(json, new TypeReference<>() {});
    }


    public static String convertObjectToJson(Object object){
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Ошибка конвертации в строку для объекта: {}",object.getClass().getName());
            return null;
        }
    }
}
