package com.meeny.application.auth;

import com.meeny.presentation.auth.dto.DevLoginRequest;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.auth.dto.TokenResponse;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.auth.AuthTokens;
import com.meeny.domain.auth.RefreshToken;
import com.meeny.domain.auth.RefreshTokenRepository;
import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenIssuer tokenIssuer;
    private final OAuthClientRegistry oauthClientRegistry;

    // 소셜 로그인: OAuth 토큰으로 사용자 정보를 받아오고, 없으면 신규 가입 후 토큰 발급
    @Transactional
    public TokenResponse socialLogin(SocialLoginRequest request) {
        OAuthUserInfo userInfo = oauthClientRegistry.getUserInfo(request.provider(), request.token());

        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), userInfo.providerId())
                .orElseGet(() -> registerNewMember(request, userInfo));

        return issueTokens(member.getId());
    }

    // 토큰 재발급: 리프레시 토큰을 검증하고 기존 토큰을 폐기한 뒤 새 토큰 쌍 발급
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        refreshToken.validate();

        Long memberId = refreshToken.getMemberId();
        refreshTokenRepository.deleteByToken(refreshTokenValue);
        return issueTokens(memberId);
    }

    // 로그아웃: 리프레시 토큰을 삭제해 재발급을 막음
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    // 개발용 로그인: OAuth 검증 없이 provider/providerId만으로 로그인 또는 가입
    @Transactional
    public TokenResponse devLogin(DevLoginRequest request) {
        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), request.providerId())
                .orElseGet(() -> memberRepository.save(
                        Member.create(request.provider(), request.providerId(), request.email(), request.nickname())
                ));
        return issueTokens(member.getId());
    }

    private Member registerNewMember(SocialLoginRequest request, OAuthUserInfo userInfo) {
        String nickname = (request.nickname() != null && !request.nickname().isBlank())
                ? request.nickname()
                : userInfo.nickname();
        return memberRepository.save(
                Member.create(request.provider(), userInfo.providerId(), userInfo.email(), nickname)
        );
    }

    private TokenResponse issueTokens(Long memberId) {
        AuthTokens tokens = tokenIssuer.issue(memberId);
        RefreshToken refreshToken = tokenIssuer.buildRefreshToken(memberId, tokens.refreshToken());
        refreshTokenRepository.save(refreshToken);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
