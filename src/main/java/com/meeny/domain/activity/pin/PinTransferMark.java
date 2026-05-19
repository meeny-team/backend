package com.meeny.domain.activity.pin;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pin_transfer_marks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pin_transfer_marks_pin_from_to",
                columnNames = {"pin_id", "from_member_id", "to_member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PinTransferMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pin_id", nullable = false)
    private Long pinId;

    @Column(name = "from_member_id", nullable = false)
    private Long fromMemberId;

    @Column(name = "to_member_id", nullable = false)
    private Long toMemberId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    private PinTransferMark(Long pinId, Long fromMemberId, Long toMemberId) {
        this.pinId = pinId;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.sentAt = LocalDateTime.now();
    }

    // 송신자가 "보냈음" 처음 누르는 시점. row 가 없을 때만 호출.
    public static PinTransferMark createSent(Long pinId, Long fromMemberId, Long toMemberId) {
        return new PinTransferMark(pinId, fromMemberId, toMemberId);
    }

    public void markReceived() {
        if (receivedAt != null) {
            throw new BusinessException(ErrorCode.TRANSFER_ALREADY_RECEIVED);
        }
        this.receivedAt = LocalDateTime.now();
    }

    // 송신자가 "취소" 를 누르려 할 때 호출. received 이후엔 거절 — service 가 delete 전에 호출.
    public void verifyCancellable() {
        if (receivedAt != null) {
            throw new BusinessException(ErrorCode.TRANSFER_ALREADY_RECEIVED);
        }
    }

    public boolean isReceived() {
        return receivedAt != null;
    }
}
