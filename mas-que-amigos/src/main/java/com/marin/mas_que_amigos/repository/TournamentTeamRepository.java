package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.TournamentTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long> {

    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    Optional<TournamentTeam> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    long countByTournamentId(Long tournamentId);

    @Query("select tt.team from TournamentTeam tt where tt.tournament.id = :tournamentId order by tt.team.nombre asc")
    List<Equipo> findTeamsByTournamentId(@Param("tournamentId") Long tournamentId);

    @Modifying
    @Query("delete from TournamentTeam tt where tt.tournament.id = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") Long tournamentId);
}
