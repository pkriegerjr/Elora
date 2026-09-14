package com.elora.common.exception;

/** 401 — credenciais inválidas, token ausente/expirado, sessão inválida. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
