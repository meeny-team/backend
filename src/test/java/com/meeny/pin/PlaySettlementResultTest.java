package com.meeny.pin;

import com.meeny.domain.activity.pin.MemberBalance;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.PlaySettlementResult;
import com.meeny.domain.activity.pin.Settlement;
import com.meeny.domain.activity.pin.SettlementType;
import com.meeny.domain.activity.pin.Split;
import com.meeny.domain.activity.pin.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 도메인 단위 테스트 — Spring 없이 PlaySettlementResult 의 결정적 계산을 검증
class PlaySettlementResultTest {

    private Pin pinOf(long amount, Long paidBy, List<Split> splits, Set<Long> playMemberIds) {
        return Pin.create(
                1L, paidBy, amount, PinCategory.FOOD, "title",
                null, null, null,
                Settlement.of(SettlementType.EQUAL, paidBy),
                splits, playMemberIds
        );
    }

    @Nested
    @DisplayName("memberBalance")
    class MemberBalanceTest {

        @Test
        @DisplayName("핀이 비어있으면 잔액은 0")
        void emptyPins() {
            assertThat(PlaySettlementResult.memberBalance(1L, List.of())).isZero();
        }

        @Test
        @DisplayName("결제자(=split 에는 없음): 양수 잔액")
        void paidWithoutShare() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = pinOf(10000L, 1L, List.of(Split.of(2L, 10000L)), members);
            assertThat(PlaySettlementResult.memberBalance(1L, List.of(pin))).isEqualTo(10000L);
        }

        @Test
        @DisplayName("분담자(=결제 안 함): 음수 잔액")
        void shareWithoutPaid() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = pinOf(10000L, 1L, List.of(Split.of(2L, 10000L)), members);
            assertThat(PlaySettlementResult.memberBalance(2L, List.of(pin))).isEqualTo(-10000L);
        }

        @Test
        @DisplayName("결제 + 분담 함께: 차액")
        void paidAndShared() {
            Set<Long> members = Set.of(1L, 2L);
            // A(1) 결제 10000, A 4000 / B 6000
            Pin pin = pinOf(10000L, 1L,
                    List.of(Split.of(1L, 4000L), Split.of(2L, 6000L)), members);
            assertThat(PlaySettlementResult.memberBalance(1L, List.of(pin))).isEqualTo(6000L);
            assertThat(PlaySettlementResult.memberBalance(2L, List.of(pin))).isEqualTo(-6000L);
        }

        @Test
        @DisplayName("관여 없는 멤버는 잔액 0")
        void unrelatedMember() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = pinOf(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members);
            assertThat(PlaySettlementResult.memberBalance(99L, List.of(pin))).isZero();
        }

        @Test
        @DisplayName("여러 핀에 걸친 누적 잔액")
        void aggregatesAcrossPins() {
            Set<Long> members = Set.of(1L, 2L);
            Pin paid1 = pinOf(20000L, 1L,
                    List.of(Split.of(1L, 10000L), Split.of(2L, 10000L)), members);
            Pin paid2 = pinOf(6000L, 2L,
                    List.of(Split.of(1L, 3000L), Split.of(2L, 3000L)), members);
            // member 1: paid 20000, share 13000 → +7000
            // member 2: paid 6000, share 13000 → -7000
            assertThat(PlaySettlementResult.memberBalance(1L, List.of(paid1, paid2))).isEqualTo(7000L);
            assertThat(PlaySettlementResult.memberBalance(2L, List.of(paid1, paid2))).isEqualTo(-7000L);
        }
    }

    @Nested
    @DisplayName("of()")
    class OfTest {

        @Test
        @DisplayName("핀 0개: total=0, 모든 balance 0, transfers 비어있음")
        void emptyPins() {
            PlaySettlementResult result = PlaySettlementResult.of(Set.of(1L, 2L), List.of());

            assertThat(result.totalAmount()).isZero();
            assertThat(result.memberBalances())
                    .extracting(MemberBalance::balance)
                    .containsOnly(0L);
            assertThat(result.memberBalances()).hasSize(2);
            assertThat(result.transfers()).isEmpty();
        }

        @Test
        @DisplayName("3명 균등: B,C 가 A 에게 송금")
        void equalSplitThreeMembers() {
            Set<Long> members = Set.of(1L, 2L, 3L);
            Pin pin = pinOf(30000L, 1L, List.of(
                    Split.of(1L, 10000L), Split.of(2L, 10000L), Split.of(3L, 10000L)
            ), members);

            PlaySettlementResult result = PlaySettlementResult.of(members, List.of(pin));

            assertThat(result.totalAmount()).isEqualTo(30000L);
            assertThat(result.transfers()).hasSize(2);
            assertThat(result.transfers()).allSatisfy(t -> {
                assertThat(t.toMemberId()).isEqualTo(1L);
                assertThat(t.amount()).isEqualTo(10000L);
            });
        }

        @Test
        @DisplayName("탈뢰자(memberIds 에 없는 사람)도 split 에 등장하면 결과에 포함")
        void includesNonMemberAppearingInSplit() {
            // play 의 현재 멤버는 {1, 2}. 그러나 과거 핀에서 3 이 분담자로 참여했음.
            Set<Long> currentMembers = Set.of(1L, 2L);
            Pin pin = pinOf(9000L, 1L, List.of(
                    Split.of(1L, 3000L), Split.of(2L, 3000L), Split.of(3L, 3000L)
            ), Set.of(1L, 2L, 3L));

            PlaySettlementResult result = PlaySettlementResult.of(currentMembers, List.of(pin));

            assertThat(result.memberBalances()).hasSize(3);
            assertThat(result.memberBalances())
                    .extracting(MemberBalance::memberId)
                    .containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("2명 단일 transfer: B 가 A 에게 빚")
        void singleTransfer() {
            Set<Long> members = Set.of(1L, 2L);
            Pin pin = pinOf(10000L, 1L,
                    List.of(Split.of(1L, 5000L), Split.of(2L, 5000L)), members);

            PlaySettlementResult result = PlaySettlementResult.of(members, List.of(pin));

            assertThat(result.transfers()).hasSize(1);
            Transfer t = result.transfers().get(0);
            assertThat(t.fromMemberId()).isEqualTo(2L);
            assertThat(t.toMemberId()).isEqualTo(1L);
            assertThat(t.amount()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("transfers: 큰 채무자가 우선, 동률이면 memberId 작은 순")
        void transferOrdering() {
            // 4명: 1 이 60000 결제, 2/3/4 가 각각 20000 빚, 1 자기 0 → 1 이 60000 받음
            // calculateTransfers 우선순위: amount 큰 순 → 동률이면 id 작은 순. 동률이라 id 순 (2,3,4).
            Set<Long> members = Set.of(1L, 2L, 3L, 4L);
            Pin pin = pinOf(60000L, 1L, List.of(
                    Split.of(2L, 20000L), Split.of(3L, 20000L), Split.of(4L, 20000L)
            ), members);

            PlaySettlementResult result = PlaySettlementResult.of(members, List.of(pin));

            assertThat(result.transfers()).hasSize(3);
            // 모두 1 에게 송금, fromMemberId 순서가 2 → 3 → 4 이어야 (id 작은 채무자 우선)
            assertThat(result.transfers())
                    .extracting(Transfer::fromMemberId)
                    .containsExactly(2L, 3L, 4L);
        }

        @Test
        @DisplayName("3:1 비대칭 — 채권자 한 명, 채무자 두 명일 때 한 채무자가 두 transfer 로 나뉘지 않음")
        void singleCreditorMultipleDebtors() {
            // A(1) 결제 30000, A=12000 / B=10000 / C=8000 분담
            // → A balance +18000 (받을), B -10000 (낼), C -8000 (낼)
            // → C 가 A 에게 8000, B 가 A 에게 10000. transfers 정확히 2개.
            Set<Long> members = Set.of(1L, 2L, 3L);
            Pin pin = pinOf(30000L, 1L, List.of(
                    Split.of(1L, 12000L), Split.of(2L, 10000L), Split.of(3L, 8000L)
            ), members);

            PlaySettlementResult result = PlaySettlementResult.of(members, List.of(pin));

            assertThat(result.transfers()).hasSize(2);
            assertThat(result.transfers())
                    .extracting(Transfer::toMemberId)
                    .containsOnly(1L);
            assertThat(result.transfers())
                    .extracting(Transfer::amount)
                    .containsExactlyInAnyOrder(10000L, 8000L);
        }
    }
}
