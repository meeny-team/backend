package com.meeny.auth.infrastructure;

import com.meeny.auth.domain.RefreshToken;
import com.meeny.auth.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpa.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpa.findByToken(token);
    }

    @Override
    public void deleteByToken(String token) {
        jpa.deleteByToken(token);
    }
}
