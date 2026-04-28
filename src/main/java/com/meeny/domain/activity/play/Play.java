package com.meeny.domain.activity.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "plays")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Play {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayType type;

    @Embedded
    private DateRange dateRange;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "play_members", joinColumns = @JoinColumn(name = "play_id"))
    @Column(name = "member_id", nullable = false)
    private Set<Long> memberIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "play_regions", joinColumns = @JoinColumn(name = "play_id"))
    @Column(name = "region", nullable = false)
    private List<String> regions;

    @ElementCollection
    @CollectionTable(name = "play_tags", joinColumns = @JoinColumn(name = "play_id"))
    @Column(name = "tag", nullable = false)
    private List<String> tags;

    public static Play create(
            Long crewId,
            Long creatorId,
            String title,
            PlayType type,
            DateRange dateRange,
            Set<Long> memberIds,
            List<String> regions,
            List<String> tags,
            String coverImage
    ) {
        Play play = new Play();
        play.crewId = crewId;
        play.createdBy = creatorId;
        play.title = title;
        play.type = type;
        play.dateRange = dateRange;
        play.memberIds = new HashSet<>(memberIds);
        play.memberIds.add(creatorId);
        play.regions = regions == null ? List.of() : List.copyOf(regions);
        play.tags = tags == null ? List.of() : List.copyOf(tags);
        play.coverImage = coverImage;
        play.createdAt = LocalDateTime.now();
        return play;
    }

    public void updateBy(
            Long memberId,
            String title,
            PlayType type,
            DateRange dateRange,
            Set<Long> memberIds,
            List<String> regions,
            List<String> tags,
            String coverImage
    ) {
        verifyAuthor(memberId);
        if (title != null && !title.isBlank()) this.title = title;
        if (type != null) this.type = type;
        if (dateRange != null) this.dateRange = dateRange;
        if (memberIds != null) {
            Set<Long> updated = new HashSet<>(memberIds);
            updated.add(this.createdBy);
            this.memberIds = updated;
        }
        if (regions != null) this.regions = List.copyOf(regions);
        if (tags != null) this.tags = List.copyOf(tags);
        if (coverImage != null) this.coverImage = coverImage.isBlank() ? null : coverImage;
    }

    public boolean isMember(Long memberId) {
        return memberIds.contains(memberId);
    }

    public void verifyAuthor(Long memberId) {
        if (!createdBy.equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PLAY_OWNER);
        }
    }
}
