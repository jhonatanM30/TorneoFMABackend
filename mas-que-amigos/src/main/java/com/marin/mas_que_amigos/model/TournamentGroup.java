package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A group of the group stage of a GROUPS_KNOCKOUT tournament (A, B, C...).
 */
@Entity
@Getter
@Setter
@Table(name = "tournament_group", uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "name"}))
public class TournamentGroup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "name", nullable = false, length = 10)
    private String name;
}
