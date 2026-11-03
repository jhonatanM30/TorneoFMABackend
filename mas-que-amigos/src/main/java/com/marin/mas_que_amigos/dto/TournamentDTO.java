package com.marin.mas_que_amigos.dto;

import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TournamentDTO {

    private Long id;
    private String name;
    private TournamentFormat format;
    private TournamentStatus status;
    private int teamCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}
