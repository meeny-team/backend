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
import com.meeny.domain.identity.SocialProvider;
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

    // 둘러보기(데모) 단일 계정 식별자. provider+providerId 유니크 제약상 (GUEST, DEMO_GUEST_PROVIDER_ID) 한 row 가 모든 둘러보기 사용자를 공유한다.
    private static final String DEMO_GUEST_PROVIDER_ID = "demo";
    private static final String DEMO_GUEST_NICKNAME = "데모 사용자";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenInvalidator refreshTokenInvalidator;
    private final TokenIssuer tokenIssuer;
    private final OAuthClientRegistry oauthClientRegistry;

    // 소셜 로그인: OAuth 토큰으로 사용자 정보를 받아오고, 없으면 신규 가입; 탈퇴 회원이면 같은 row를 재활성화 (provider+providerId 유니크 제약 우회)
    @Transactional
    public TokenResponse socialLogin(SocialLoginRequest request) {
        OAuthUserInfo userInfo = oauthClientRegistry.getUserInfo(request.provider(), request.token());

        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), userInfo.providerId())
                .map(existing -> {
                    if (existing.isDeleted()) {
                        existing.reactivate(userInfo.email(), resolveNickname(request, userInfo));
                    }
                    return existing;
                })
                .orElseGet(() -> registerNewMember(request, userInfo));

        return issueTokens(member.getId());
    }

    // 토큰 재발급(rotation + reuse detection):
    // - 정상: 옛 토큰을 used 마킹(영속화는 dirty checking) 후 새 토큰 쌍 발급
    // - 이미 used인 토큰이 또 들어오면 탈취로 간주 → 해당 회원의 모든 refresh 토큰 강제 무효화
    //   (별도 트랜잭션에서 commit 후 예외를 던져 클라이언트에 재로그인 유도)
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (refreshToken.isUsed()) {
            refreshTokenInvalidator.invalidateAllForMember(refreshToken.getMemberId());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        refreshToken.validate();
        refreshToken.markUsed();

        return issueTokens(refreshToken.getMemberId());
    }

    // 로그아웃: 리프레시 토큰을 삭제해 재발급을 막음
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    // 둘러보기(게스트) 로그인: 인증 없이 공유 데모 계정으로 토큰 발급.
    // App Review 리뷰어가 OTP 없이 앱 전체 기능을 확인할 수 있도록 하는 진입점이기도 하다.
    @Transactional
    public TokenResponse guestLogin() {
        Member member = memberRepository
                .findByProviderAndProviderId(SocialProvider.GUEST, DEMO_GUEST_PROVIDER_ID)
                .map(existing -> {
                    if (existing.isDeleted()) {
                        existing.reactivate(null, DEMO_GUEST_NICKNAME);
                    }
                    return existing;
                })
                .orElseGet(() -> memberRepository.save(
                        Member.create(SocialProvider.GUEST, DEMO_GUEST_PROVIDER_ID, null, DEMO_GUEST_NICKNAME)
                ));
        return issueTokens(member.getId());
    }

    // 개발용 로그인: OAuth 검증 없이 provider/providerId만으로 로그인 또는 가입; 탈퇴 회원도 동일하게 재활성화
    @Transactional
    public TokenResponse devLogin(DevLoginRequest request) {
        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), request.providerId())
                .map(existing -> {
                    if (existing.isDeleted()) {
                        existing.reactivate(request.email(), request.nickname());
                    }
                    return existing;
                })
                .orElseGet(() -> memberRepository.save(
                        Member.create(request.provider(), request.providerId(), request.email(), request.nickname())
                ));
        return issueTokens(member.getId());
    }

    private Member registerNewMember(SocialLoginRequest request, OAuthUserInfo userInfo) {
        return memberRepository.save(
                Member.create(request.provider(), userInfo.providerId(), userInfo.email(), resolveNickname(request, userInfo))
        );
    }

    private String resolveNickname(SocialLoginRequest request, OAuthUserInfo userInfo) {
        return (request.nickname() != null && !request.nickname().isBlank())
                ? request.nickname()
                : userInfo.nickname();
    }

    private TokenResponse issueTokens(Long memberId) {
        AuthTokens tokens = tokenIssuer.issue(memberId);
        RefreshToken refreshToken = tokenIssuer.buildRefreshToken(memberId, tokens.refreshToken());
        refreshTokenRepository.save(refreshToken);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
