package com.elora.common.exception;

/** 404 — ex.: usuário/notificação não encontrado ou fora do dono. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
