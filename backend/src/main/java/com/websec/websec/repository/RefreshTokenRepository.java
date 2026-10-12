package com.websec.websec.repository;

import com.websec.websec.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// row lock so two parallel refreshes with the same token can't both succeed
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "user")
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update RefreshToken t set t.revoked = true where t.user.id = :userId")
	void revokeAllForUser(@Param("userId") Long userId);

	@Modifying
	@Query("delete from RefreshToken t where t.expiresAt < :now")
	int deleteExpired(@Param("now") Instant now);
}
