package com.meeny.infrastructure.oauth.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

// JWKS 엔드포인트를 흉내내는 테스트 서버 + 같은 RSA 키페어로 토큰을 서명할 수 있는 헬퍼.
// Google/Apple 클라이언트가 NimbusJwtDecoder로 외부 JWKS를 가져와 서명을 검증하므로,
// 이 헬퍼로 in-process JWKS를 띄우고 일치하는 키로 토큰을 서명해 진짜 검증 흐름을 그대로 통과시킨다.
class JwksTestServer {

    private final MockWebServer server;
    private final RSAKey rsaJwk;

    JwksTestServer() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        this.rsaJwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();

        this.server = new MockWebServer();
        this.server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(new JWKSet(rsaJwk.toPublicJWK()).toString());
            }
        });
        this.server.start();
    }

    String url() {
        return server.url("/").toString();
    }

    void shutdown() throws IOException {
        server.shutdown();
    }

    // 주어진 claim들로 RS256 토큰을 서명. JwksTestServer가 노출한 JWKS의 공개키로 검증 가능.
    String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claims
        );
        jwt.sign(new RSASSASigner(rsaJwk));
        return jwt.serialize();
    }
}
