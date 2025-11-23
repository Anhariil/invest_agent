package ru.invest.agent.telegram.exception;

public class InvestServiceException extends ApplicationException {

    private static final String ERROR_CODE = "InvestDataNotCorrect";

    public InvestServiceException(String message) {
        super(ERROR_CODE,message,null,null);
    }
}
