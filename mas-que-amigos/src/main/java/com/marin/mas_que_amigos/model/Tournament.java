package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A competition with a fixed play format and a set of enrolled teams. Every
 * match belongs to exactly one tournament.
 */
@Entity
@Getter
@Setter
@Table(name = "tournament")
public class Tournament implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "format", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TournamentFormat format;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // GROUPS_KNOCKOUT only: teams that qualify from each group (1 or 2), set when the groups are formed.
    @Column(name = "qualifiers_per_group")
    private Integer qualifiersPerGroup;
}
