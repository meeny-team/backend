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

    public static Member create(SocialProvider provider, String providerId, String email, String nickname) {
        Member member = new Member();
        member.provider = provider;
        member.providerId = providerId;
        member.email = email;
        member.nickname = (nickname != null && !nickname.isBlank()) ? nickname : "사용자";
        return member;
    }

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
