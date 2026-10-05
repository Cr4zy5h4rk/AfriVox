package com.afrivox.app.service;

import java.io.Serial;

/**
 * Jeton de rafraichissement absent, inconnu, expire, revoque ou rejoue.
 * Le message ne doit jamais contenir le jeton.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
