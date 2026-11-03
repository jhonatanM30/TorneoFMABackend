package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // The protected "Histórico" tournament is identified by its internal format.
    Optional<Tournament> findFirstByFormat(TournamentFormat format);

    // Locking read: state-changing operations (enroll, start, finish, create a
    // match) must see the latest committed status and serialize with each other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tournament t where t.id = :id")
    Optional<Tournament> findByIdForUpdate(@Param("id") Long id);
}
