package com.elora.common.exception;

/** 422/400 — violação de regra de negócio (ex.: CPF duplicado, transição de status inválida). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
