package com.marin.mas_que_amigos.exception;

/**
 * Login failed or the account behind a session no longer exists. Answered with 401.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
