package com.meeny.infrastructure.oauth;

import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthClientRegistry {

    private final Map<SocialProvider, OAuthClient> clients;

    public OAuthClientRegistry(List<OAuthClient> oauthClients) {
        this.clients = oauthClients.stream()
                .collect(Collectors.toMap(OAuthClient::provider, Function.identity()));
    }

    public OAuthUserInfo getUserInfo(SocialProvider provider, String token) {
        OAuthClient client = getClient(provider);
        return client.getUserInfo(token);
    }

    public OAuthClient getClient(SocialProvider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
        return client;
    }

    public boolean supports(SocialProvider provider) {
        return clients.containsKey(provider);
    }
}
