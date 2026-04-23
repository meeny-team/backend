package com.meeny.auth.infrastructure;

import com.meeny.auth.domain.RefreshToken;
import com.meeny.auth.domain.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
}
