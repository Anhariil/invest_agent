package ru.invest.agent.telegram.exception;

import org.springframework.http.HttpStatusCode;

import java.util.Map;

public class ApplicationException extends Exception {

    private String message;
    private String code;
    private Map<String, Object> meta;

    public ApplicationException(String code, String message, Map<String, Object> meta, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = code;
        this.meta = meta;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "ApplicationException{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}