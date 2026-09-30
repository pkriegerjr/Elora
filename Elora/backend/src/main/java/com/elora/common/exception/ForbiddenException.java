package com.elora.common.exception;

/** 403 — autenticado mas sem permissão (ex.: ver dados de outro usuário sem perfil staff). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
