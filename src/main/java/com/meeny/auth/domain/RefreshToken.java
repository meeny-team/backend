package com.meeny.auth.domain;

import com.meeny.global.exception.BusinessException;
import com.meeny.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static RefreshToken create(Long memberId, String token, long expireMs) {
        RefreshToken rt = new RefreshToken();
        rt.memberId = memberId;
        rt.token = token;
        rt.expiresAt = LocalDateTime.now().plusSeconds(expireMs / 1000);
        return rt;
    }

    public void validate() {
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
    }
}
