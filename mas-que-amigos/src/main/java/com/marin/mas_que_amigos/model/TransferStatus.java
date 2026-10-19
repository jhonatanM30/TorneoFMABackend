package com.marin.mas_que_amigos.model;

/**
 * Lifecycle of a {@link PlayerTransfer}: it starts as PENDING and is resolved
 * exactly once, either CONFIRMED (the player moves) or REJECTED (manually or
 * automatically when another request of the same player is confirmed).
 */
public enum TransferStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
