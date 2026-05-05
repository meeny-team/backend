package com.meeny.domain.auth;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
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

    // 토큰이 갱신에 한 번 사용되면 시간이 기록되고, 이후 같은 토큰이 다시 들어오면 탈취로 간주(reuse detection).
    @Column
    private LocalDateTime usedAt;

    public static RefreshToken create(Long memberId, String token, long expireMs) {
        RefreshToken rt = new RefreshToken();
        rt.memberId = memberId;
        rt.token = token;
        rt.expiresAt = LocalDateTime.now().plus(Duration.ofMillis(expireMs));
        return rt;
    }

    public void validate() {
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
