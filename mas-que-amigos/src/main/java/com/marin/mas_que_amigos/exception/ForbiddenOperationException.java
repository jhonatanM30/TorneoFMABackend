package com.marin.mas_que_amigos.exception;

/**
 * The caller is authenticated but a business rule forbids the operation (for
 * example a DT asking for a transfer into a team that is not theirs). Answered with 403.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
