package com.meeny.domain.activity.pin;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Pin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "play_id", nullable = false)
    private Long playId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PinCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String memo;

    @Column(length = 200)
    private String location;

    @Embedded
    private Settlement settlement;

    @ElementCollection
    @CollectionTable(name = "pin_images", joinColumns = @JoinColumn(name = "pin_id"))
    @Column(name = "image_url", nullable = false)
    private List<String> images;

    @ElementCollection
    @CollectionTable(name = "pin_splits", joinColumns = @JoinColumn(name = "pin_id"))
    private List<Split> splits;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Pin create(
            Long playId,
            Long authorId,
            long amount,
            PinCategory category,
            String title,
            String memo,
            String location,
            List<String> images,
            Settlement settlement,
            List<Split> splits,
            Set<Long> playMemberIds
    ) {
        verifySettlement(amount, settlement, splits, playMemberIds);

        Pin pin = new Pin();
        pin.playId = playId;
        pin.authorId = authorId;
        pin.amount = amount;
        pin.category = category;
        pin.title = title;
        pin.memo = memo;
        pin.location = location;
        pin.images = images == null ? List.of() : List.copyOf(images);
        pin.settlement = settlement;
        pin.splits = List.copyOf(splits);
        pin.createdAt = LocalDateTime.now();
        return pin;
    }

    public void updateBy(
            Long memberId,
            Long newAmount,
            PinCategory category,
            String title,
            String memo,
            String location,
            List<String> images,
            Settlement settlement,
            List<Split> splits,
            Set<Long> playMemberIds
    ) {
        verifyAuthor(memberId);

        long resolvedAmount = newAmount != null ? newAmount : this.amount;
        Settlement resolvedSettlement = settlement != null ? settlement : this.settlement;
        List<Split> resolvedSplits = splits != null ? splits : this.splits;

        // 메타데이터만 바뀌어도 매번 검증해, Play 멤버 변동으로 좀비가 된 split/payer가 통과되지 않게 한다.
        verifySettlement(resolvedAmount, resolvedSettlement, resolvedSplits, playMemberIds);

        this.amount = resolvedAmount;
        if (category != null) this.category = category;
        if (title != null && !title.isBlank()) this.title = title;
        if (memo != null) this.memo = memo.isBlank() ? null : memo;
        if (location != null) this.location = location.isBlank() ? null : location;
        if (images != null) this.images = List.copyOf(images);
        if (settlement != null) this.settlement = settlement;
        if (splits != null) this.splits = List.copyOf(splits);
    }

    public void verifyAuthor(Long memberId) {
        if (!authorId.equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PIN_OWNER);
        }
    }

    private static void verifySettlement(
            long amount,
            Settlement settlement,
            List<Split> splits,
            Set<Long> playMemberIds
    ) {
        if (settlement == null || !playMemberIds.contains(settlement.getPaidBy())) {
            throw new BusinessException(ErrorCode.INVALID_PIN_PAYER);
        }
        if (splits == null || splits.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PIN_SPLITS);
        }
        for (Split split : splits) {
            if (!playMemberIds.contains(split.getUserId())) {
                throw new BusinessException(ErrorCode.INVALID_PIN_SPLITS);
            }
        }
        long sum = splits.stream().mapToLong(Split::getAmount).sum();
        if (sum != amount) {
            throw new BusinessException(ErrorCode.INVALID_SPLIT_SUM);
        }
    }
}
