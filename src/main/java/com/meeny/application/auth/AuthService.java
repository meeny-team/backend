package com.meeny.application.auth;

import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.auth.dto.TokenResponse;
import com.meeny.domain.auth.AuthTokens;
import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.auth.RefreshToken;
import com.meeny.domain.auth.RefreshTokenRepository;
import com.meeny.domain.member.Member;
import com.meeny.domain.member.MemberRepository;
import com.meeny.domain.member.SocialProvider;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
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
        String nickname = (request.nickname() != null && !request.nickname().isBlank())
                ? request.nickname()
                : userInfo.nickname();
        return memberRepository.save(
                Member.create(request.provider(), userInfo.providerId(), userInfo.email(), nickname)
        );
    }

    private OAuthClient findOAuthClient(SocialProvider provider) {
        return oauthClients.stream()
                .filter(c -> c.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER));
    }

    private TokenResponse issueTokens(Long memberId) {
        AuthTokens tokens = tokenIssuer.issue(memberId);
        RefreshToken refreshToken = tokenIssuer.buildRefreshToken(memberId, tokens.refreshToken());
        refreshTokenRepository.save(refreshToken);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
