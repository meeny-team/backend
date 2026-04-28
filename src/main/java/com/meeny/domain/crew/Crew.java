package com.meeny.domain.crew;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "crews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Embedded
    private InviteCode inviteCode;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "crew_members",
            joinColumns = @JoinColumn(name = "crew_id")
    )
    @Column(name = "member_id", nullable = false)
    private Set<Long> memberIds = new HashSet<>();

    public static Crew create(String name, String coverImage, Long creatorId, InviteCode inviteCode) {
        Crew crew = new Crew();
        crew.name = name;
        crew.coverImage = coverImage;
        crew.createdBy = creatorId;
        crew.inviteCode = inviteCode;
        crew.createdAt = LocalDateTime.now();
        crew.memberIds.add(creatorId);
        return crew;
    }

    public void join(Long memberId) {
        if (memberIds.contains(memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CREW);
        }
        memberIds.add(memberId);
    }

    public void leave(Long memberId) {
        if (!memberIds.contains(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }
        memberIds.remove(memberId);
    }

    public void updateBy(Long memberId, String name, String coverImage) {
        verifyOwner(memberId);
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (coverImage != null) {
            this.coverImage = coverImage.isBlank() ? null : coverImage;
        }
    }

    public boolean isMember(Long memberId) {
        return memberIds.contains(memberId);
    }

    public void verifyMember(Long memberId) {
        if (!isMember(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }
    }

    public void verifyOwner(Long memberId) {
        if (!createdBy.equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_OWNER);
        }
    }
}
