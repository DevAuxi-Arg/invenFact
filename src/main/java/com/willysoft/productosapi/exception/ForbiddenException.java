package com.willysoft.productosapi.exception;

/**
 * Se lanza cuando una acción está prohibida por una regla de negocio de roles
 * (p. ej. un CO-ADMIN intentando gestionar a otro administrador).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
