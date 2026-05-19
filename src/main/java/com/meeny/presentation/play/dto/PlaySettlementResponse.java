package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.pin.MemberBalance;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinTransferMark;
import com.meeny.domain.activity.pin.PlaySettlementResult;
import com.meeny.domain.activity.pin.Split;
import com.meeny.domain.activity.pin.Transfer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record PlaySettlementResponse(
        Long playId,
        LocalDateTime settledAt,
        long totalAmount,
        List<MemberBalanceDto> memberBalances,
        List<TransferDto> transfers,
        List<PinTransferDto> pinTransfers
) {
    // 닉네임은 memberId → displayName 맵으로 한 번에 주입; 탈퇴자는 "(탈퇴한 사용자)"로 표시되도록 호출 측에서 미리 변환
    public static PlaySettlementResponse from(
            Long playId,
            LocalDateTime settledAt,
            PlaySettlementResult result,
            List<Pin> pins,
            List<PinTransferMark> marks,
            Map<Long, String> nicknames
    ) {
        Function<Long, String> nicknameOf = id -> nicknames.getOrDefault(id, "");
        return new PlaySettlementResponse(
                playId,
                settledAt,
                result.totalAmount(),
                result.memberBalances().stream()
                        .map(b -> MemberBalanceDto.from(b, nicknameOf.apply(b.memberId())))
                        .toList(),
                result.transfers().stream()
                        .map(t -> TransferDto.from(t, nicknameOf.apply(t.fromMemberId()), nicknameOf.apply(t.toMemberId())))
                        .toList(),
                buildPinTransfers(pins, marks, nicknameOf)
        );
    }

    // 핀별 (fromMemberId → paidBy) 단위 송금 상태. paidBy 자신과 amount==0 split 은 제외.
    private static List<PinTransferDto> buildPinTransfers(
            List<Pin> pins,
            List<PinTransferMark> marks,
            Function<Long, String> nicknameOf
    ) {
        Map<String, PinTransferMark> markMap = marks.stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.getPinId() + ":" + m.getFromMemberId() + ":" + m.getToMemberId(),
                        m -> m
                ));
        return pins.stream()
                .flatMap(pin -> {
                    Long paidBy = pin.getSettlement().getPaidBy();
                    return pin.getSplits().stream()
                            .filter(s -> !s.getUserId().equals(paidBy) && s.getAmount() > 0)
                            .map(s -> {
                                PinTransferMark mark = markMap.get(pin.getId() + ":" + s.getUserId() + ":" + paidBy);
                                LocalDateTime sentAt = mark != null ? mark.getSentAt() : null;
                                LocalDateTime receivedAt = mark != null ? mark.getReceivedAt() : null;
                                return new PinTransferDto(
                                        pin.getId(),
                                        s.getUserId(),
                                        nicknameOf.apply(s.getUserId()),
                                        paidBy,
                                        nicknameOf.apply(paidBy),
                                        s.getAmount(),
                                        sentAt,
                                        receivedAt
                                );
                            });
                })
                .toList();
    }

    public record MemberBalanceDto(
            Long memberId,
            String nickname,
            long totalPaid,
            long totalShare,
            long balance
    ) {
        public static MemberBalanceDto from(MemberBalance b, String nickname) {
            return new MemberBalanceDto(b.memberId(), nickname, b.totalPaid(), b.totalShare(), b.balance());
        }
    }

    public record TransferDto(
            Long fromMemberId,
            String fromNickname,
            Long toMemberId,
            String toNickname,
            long amount
    ) {
        public static TransferDto from(Transfer t, String fromNickname, String toNickname) {
            return new TransferDto(t.fromMemberId(), fromNickname, t.toMemberId(), toNickname, t.amount());
        }
    }

    public record PinTransferDto(
            Long pinId,
            Long fromMemberId,
            String fromNickname,
            Long toMemberId,
            String toNickname,
            long amount,
            LocalDateTime sentAt,
            LocalDateTime receivedAt
    ) {
    }
}
