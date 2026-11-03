package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.TournamentDTO;
import com.marin.mas_que_amigos.model.Tournament;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

    public TournamentDTO toDTO(Tournament tournament, long teamCount) {
        TournamentDTO dto = new TournamentDTO();
        dto.setId(tournament.getId());
        dto.setName(tournament.getName());
        dto.setFormat(tournament.getFormat());
        dto.setStatus(tournament.getStatus());
        dto.setTeamCount((int) teamCount);
        dto.setCreatedAt(tournament.getCreatedAt());
        dto.setStartedAt(tournament.getStartedAt());
        return dto;
    }
}
