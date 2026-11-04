package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.BracketTie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BracketTieRepository extends JpaRepository<BracketTie, Long> {

    List<BracketTie> findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(Long tournamentId);

    boolean existsByTournamentId(Long tournamentId);

    Optional<BracketTie> findByMatchId(Long matchId);

    boolean existsByMatchId(Long matchId);
}
