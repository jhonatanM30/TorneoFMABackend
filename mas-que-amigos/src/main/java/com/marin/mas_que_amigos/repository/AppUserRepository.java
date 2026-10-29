package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Usernames are stored in lowercase; callers normalize before querying.
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findByTeamId(Long teamId);

    List<AppUser> findAllByOrderByUsernameAsc();
}
