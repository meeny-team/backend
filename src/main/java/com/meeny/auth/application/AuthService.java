package com.meeny.auth.application;

import com.meeny.auth.application.dto.SocialLoginRequest;
import com.meeny.auth.application.dto.TokenResponse;
import com.meeny.auth.domain.AuthTokens;
import com.meeny.auth.domain.OAuthClient;
import com.meeny.auth.domain.OAuthUserInfo;
import com.meeny.auth.domain.RefreshToken;
import com.meeny.auth.domain.RefreshTokenRepository;
import com.meeny.auth.domain.TokenIssuer;
import com.meeny.global.exception.BusinessException;
import com.meeny.global.exception.ErrorCode;
import com.meeny.global.jwt.JwtProperties;
import com.meeny.member.domain.Member;
import com.meeny.member.domain.MemberRepository;
import com.meeny.member.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenIssuer tokenIssuer;
    private final JwtProperties jwtProperties;
    private final List<OAuthClient> oauthClients;

    @Transactional
    public TokenResponse socialLogin(SocialLoginRequest request) {
        OAuthClient client = findOAuthClient(request.provider());
        OAuthUserInfo userInfo = client.getUserInfo(request.token());

        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), userInfo.providerId())
                .orElseGet(() -> registerNewMember(request, userInfo));

        return issueTokens(member.getId());
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        refreshToken.validate();

        Long memberId = refreshToken.getMemberId();
        refreshTokenRepository.deleteByToken(refreshTokenValue);
        return issueTokens(memberId);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    private Member registerNewMember(SocialLoginRequest request, OAuthUserInfo userInfo) {
        String nickname = resolveNickname(request.nickname(), userInfo.nickname());
        return memberRepository.save(
                Member.create(request.provider(), userInfo.providerId(), userInfo.email(), nickname)
        );
    }

    private String resolveNickname(String requestNickname, String providerNickname) {
        if (requestNickname != null && !requestNickname.isBlank()) return requestNickname;
        if (providerNickname != null && !providerNickname.isBlank()) return providerNickname;
        return "사용자";
    }

    private OAuthClient findOAuthClient(SocialProvider provider) {
        return oauthClients.stream()
                .filter(c -> c.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER));
    }

    private TokenResponse issueTokens(Long memberId) {
        AuthTokens tokens = tokenIssuer.issue(memberId);
        RefreshToken refreshToken = RefreshToken.create(memberId, tokens.refreshToken(), jwtProperties.refreshTokenExpireMs());
        refreshTokenRepository.save(refreshToken);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
