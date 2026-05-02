package com.meeny.domain.identity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member {

    private static final String DELETED_NICKNAME = "(탈퇴한 사용자)";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(length = 100)
    private String bio;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 회원 생성: 소셜 식별자로 가입하며, 닉네임이 비어있으면 기본값 "사용자"로 채움
    public static Member create(SocialProvider provider, String providerId, String email, String nickname) {
        Member member = new Member();
        member.provider = provider;
        member.providerId = providerId;
        member.email = email;
        member.nickname = (nickname != null && !nickname.isBlank()) ? nickname : "사용자";
        return member;
    }

    // 프로필 부분 수정: null이거나 빈 문자열인 필드는 변경하지 않음 (이미지/소개는 빈 문자열을 null로 정규화)
    public void updateProfile(String nickname, String profileImage, String bio) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImage != null) {
            this.profileImage = profileImage.isBlank() ? null : profileImage;
        }
        if (bio != null) {
            this.bio = bio.isBlank() ? null : bio;
        }
    }

    // 탈퇴 처리: 과거 정산 기록의 외래키 무결성을 위해 삭제 대신 시점만 기록
    public void withdraw() {
        if (deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    // 같은 소셜 계정으로 다시 로그인한 탈퇴 회원의 계정을 되살림 (provider+providerId 유니크 제약 우회)
    public void reactivate(String email, String nickname) {
        if (deletedAt == null) return;
        this.deletedAt = null;
        this.email = email;
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // 표시용 닉네임: 탈퇴자는 "(탈퇴한 사용자)"로 마스킹해 다른 멤버에게 노출
    public String getDisplayNickname() {
        return isDeleted() ? DELETED_NICKNAME : nickname;
    }
}
