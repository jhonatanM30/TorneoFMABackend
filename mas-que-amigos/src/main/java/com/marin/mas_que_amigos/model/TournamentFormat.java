package com.marin.mas_que_amigos.model;

/**
 * How a tournament is played. The format is chosen before the tournament
 * starts and stays locked afterwards. LEGACY is internal: it only labels the
 * protected "Histórico" tournament and cannot be selected through the API.
 */
public enum TournamentFormat {
    LEAGUE, GROUPS_KNOCKOUT, KNOCKOUT, LEGACY
}
