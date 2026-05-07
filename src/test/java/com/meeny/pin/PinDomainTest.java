package com.meeny.pin;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.Settlement;
import com.meeny.domain.activity.pin.SettlementType;
import com.meeny.domain.activity.pin.Split;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pin 도메인의 invariant 검증 — Spring 없이 빠르게
class PinDomainTest {

    private static final Long PLAY_ID = 100L;
    private static final Long AUTHOR = 1L;

    private Pin createPin(long amount, Long paidBy, List<Split> splits, Set<Long> playMembers) {
        return Pin.create(
                PLAY_ID, AUTHOR, amount, PinCategory.FOOD, "title",
                null, null, null,
                Settlement.of(SettlementType.EQUAL, paidBy),
                splits, playMembers
        );
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("정상 생성")
        void ok() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = createPin(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members);

            assertThat(pin.getAmount()).isEqualTo(10000L);
            assertThat(pin.getSettlement().getPaidBy()).isEqualTo(1L);
            assertThat(pin.getSplits()).hasSize(2);
            assertThat(pin.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("paidBy 가 play 멤버가 아니면 INVALID_PIN_PAYER")
        void paidByNotMember() {
            Set<Long> members = Set.of(1L, 2L);
            assertThatThrownBy(() -> createPin(10000L, 99L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PIN_PAYER);
        }

        @Test
        @DisplayName("splits 가 비어있으면 INVALID_PIN_SPLITS")
        void emptySplits() {
            Set<Long> members = Set.of(1L, 2L);
            assertThatThrownBy(() -> createPin(10000L, 1L, List.of(), members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PIN_SPLITS);
        }

        @Test
        @DisplayName("splits 의 userId 가 play 멤버가 아니면 INVALID_PIN_SPLITS")
        void splitUserNotMember() {
            Set<Long> members = Set.of(1L, 2L);
            assertThatThrownBy(() -> createPin(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(99L, 5000L)), members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PIN_SPLITS);
        }

        @Test
        @DisplayName("splits 합이 amount 와 다르면 INVALID_SPLIT_SUM")
        void splitSumMismatch() {
            Set<Long> members = Set.of(1L, 2L);
            assertThatThrownBy(() -> createPin(10000L, 1L,
                    List.of(Split.of(1L, 4000L), Split.of(2L, 5000L)), members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_SPLIT_SUM);
        }

        @Test
        @DisplayName("amount 0, splits 합 0 도 정상 (무료 모임 등)")
        void zeroAmountAllowed() {
            Set<Long> members = Set.of(1L);
            Pin pin = createPin(0L, 1L, List.of(Split.of(1L, 0L)), members);
            assertThat(pin.getAmount()).isZero();
        }

        @Test
        @DisplayName("images null 이면 빈 리스트로 저장")
        void nullImages() {
            Set<Long> members = Set.of(1L);
            Pin pin = Pin.create(
                    PLAY_ID, AUTHOR, 1000L, PinCategory.FOOD, "title",
                    null, null, null,
                    Settlement.of(SettlementType.EQUAL, 1L),
                    List.of(Split.of(1L, 1000L)), members
            );
            assertThat(pin.getImages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("verifyAuthor()")
    class VerifyAuthor {

        @Test
        @DisplayName("작성자 본인이면 통과")
        void ownerOk() {
            Set<Long> members = Set.of(1L);
            Pin pin = createPin(1000L, 1L, List.of(Split.of(1L, 1000L)), members);

            // 예외 없이 통과
            pin.verifyAuthor(AUTHOR);
        }

        @Test
        @DisplayName("작성자 아니면 NOT_PIN_OWNER")
        void notOwner() {
            Set<Long> members = Set.of(1L);
            Pin pin = createPin(1000L, 1L, List.of(Split.of(1L, 1000L)), members);

            assertThatThrownBy(() -> pin.verifyAuthor(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_PIN_OWNER);
        }
    }

    @Nested
    @DisplayName("updateBy()")
    class UpdateBy {

        @Test
        @DisplayName("작성자 아니면 NOT_PIN_OWNER (검증 후 변경 안 됨)")
        void rejectsNonOwner() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = createPin(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members);

            assertThatThrownBy(() -> pin.updateBy(99L, 20000L,
                    null, null, null, null, null, null,
                    List.of(Split.of(1L, 10000L), Split.of(2L, 10000L)), members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_PIN_OWNER);
            assertThat(pin.getAmount()).isEqualTo(10000L);
        }

        @Test
        @DisplayName("amount 만 변경: splits 가 새 amount 와 안 맞으면 INVALID_SPLIT_SUM")
        void amountWithoutSplitsRecomputesValidation() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = createPin(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members);

            // 기존 splits (5000+5000=10000) 그대로인데 amount 만 20000 → 합 불일치
            assertThatThrownBy(() -> pin.updateBy(AUTHOR, 20000L,
                    null, null, null, null, null, null, null, members))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_SPLIT_SUM);
        }

        @Test
        @DisplayName("memo: blank 입력은 null 로 정규화")
        void blankMemoBecomesNull() {
            Set<Long> members = Set.of(1L);
            Pin pin = createPin(1000L, 1L, List.of(Split.of(1L, 1000L)), members);

            pin.updateBy(AUTHOR, null, null, null, "   ", null, null, null, null, members);

            assertThat(pin.getMemo()).isNull();
        }

        @Test
        @DisplayName("Play 멤버 변동으로 split userId 가 더 이상 멤버가 아니면 INVALID_PIN_SPLITS")
        void splitUserDroppedFromPlay() {
            Set<Long> originalMembers = Set.of(1L, 2L);
            Pin pin = createPin(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), originalMembers);

            // 멤버에서 2 가 빠진 시점에 update — 기존 splits 가 좀비
            Set<Long> shrunkMembers = Set.of(1L);
            assertThatThrownBy(() -> pin.updateBy(AUTHOR, null, null, null, null, null, null, null, null, shrunkMembers))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PIN_SPLITS);
        }
    }
}
