package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.PlayerTransfer;
import com.marin.mas_que_amigos.model.TransferStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerTransferRepository extends JpaRepository<PlayerTransfer, Long> {

    // One derived query per filter combination (all newest first; id breaks ties
    // between requests created in the same instant) instead of a nullable-enum
    // JPQL parameter, which is fragile across Hibernate/MySQL versions.
    List<PlayerTransfer> findAllByOrderByRequestedAtDescIdDesc();

    List<PlayerTransfer> findByStatusOrderByRequestedAtDescIdDesc(TransferStatus status);

    List<PlayerTransfer> findByPlayerIdOrderByRequestedAtDescIdDesc(Long playerId);

    List<PlayerTransfer> findByStatusAndPlayerIdOrderByRequestedAtDescIdDesc(TransferStatus status, Long playerId);

    // Scalar read (no entity enters the persistence context) used to know which
    // player row to lock before re-reading the transfer under lock.
    @Query("select t.player.id from PlayerTransfer t where t.id = :id")
    Optional<Long> findPlayerIdById(@Param("id") Long id);

    // Locking read: always sees the latest committed status, unlike a plain
    // read inside a REPEATABLE READ transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PlayerTransfer t where t.id = :id")
    Optional<PlayerTransfer> findByIdForUpdate(@Param("id") Long id);

    // Auto-rejection when another request of the same player is confirmed.
    @Modifying(flushAutomatically = true)
    @Query("update PlayerTransfer t set t.status = com.marin.mas_que_amigos.model.TransferStatus.REJECTED, "
            + "t.resolvedAt = :resolvedAt "
            + "where t.player.id = :playerId and t.id <> :exceptId "
            + "and t.status = com.marin.mas_que_amigos.model.TransferStatus.PENDING")
    int rejectOtherPending(@Param("playerId") Long playerId, @Param("exceptId") Long exceptId,
            @Param("resolvedAt") LocalDateTime resolvedAt);

    // Conditional update so a manual rejection can never overwrite a request
    // that was confirmed concurrently. Returns 0 when it was no longer PENDING.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlayerTransfer t set t.status = com.marin.mas_que_amigos.model.TransferStatus.REJECTED, "
            + "t.resolvedAt = :resolvedAt "
            + "where t.id = :id and t.status = com.marin.mas_que_amigos.model.TransferStatus.PENDING")
    int rejectIfPending(@Param("id") Long id, @Param("resolvedAt") LocalDateTime resolvedAt);
}
