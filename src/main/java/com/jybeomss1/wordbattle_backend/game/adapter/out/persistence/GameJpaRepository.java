package com.jybeomss1.wordbattle_backend.game.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * GameJpaEntity용 JPA Repository
 */
@Repository
public interface GameJpaRepository extends JpaRepository<GameJpaEntity, java.util.UUID> {
}