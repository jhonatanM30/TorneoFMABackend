package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.TournamentGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, Long> {

    List<TournamentGroup> findByTournamentIdOrderByNameAsc(Long tournamentId);

    boolean existsByTournamentId(Long tournamentId);
}
