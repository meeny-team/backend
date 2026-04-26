package com.meeny.infrastructure.auth;

import com.meeny.domain.auth.RefreshToken;
import com.meeny.domain.auth.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
}
