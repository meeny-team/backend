package com.meeny.infrastructure.postgres;

import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.domain.auth.RefreshTokenRepository;
import com.meeny.domain.identity.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class RepositoryRegistry {

    private final MemberRepository memberRepository;
    private final CrewRepository crewRepository;
    private final PlayRepository playRepository;
    private final PinRepository pinRepository;
    private final RefreshTokenRepository refreshTokenRepository;
}
