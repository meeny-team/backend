package com.meeny.application.auth;

import com.meeny.domain.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 탈취 감지 시 토큰 무효화는 호출 측이 throw로 트랜잭션을 롤백시켜도 살아남아야 한다.
// REQUIRES_NEW 로 별도 트랜잭션을 열어 즉시 커밋한 뒤 호출자가 예외를 던지도록 분리.
@Component
@RequiredArgsConstructor
public class RefreshTokenInvalidator {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateAllForMember(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
