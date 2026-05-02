package com.meeny.domain.identity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member {

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
}
