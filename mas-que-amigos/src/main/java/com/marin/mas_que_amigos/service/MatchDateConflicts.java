package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * The rule "a team does not play twice on the same day" (across tournaments),
 * shared by everything that generates matches in bulk: league calendars, group
 * calendars and the first round of a bracket.
 */
@Component
public class MatchDateConflicts {

    private final PartidoRepository partidoRepository;

    public MatchDateConflicts(PartidoRepository partidoRepository) {
        this.partidoRepository = partidoRepository;
    }

    /**
     * @throws BusinessException naming the team and the date when the team already plays that day
     */
    public void requireFree(LocalDate date, Equipo team) {
        if (partidoRepository.existePartidoEnFechaParaEquipo(date, team.getId())) {
            throw new BusinessException("El equipo " + team.getNombre() + " ya tiene un partido el " + date
                    + ": elige otra fecha de inicio o intervalo.");
        }
    }
}
