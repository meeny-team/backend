package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.pin.MemberBalance;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinTransferMark;
import com.meeny.domain.activity.pin.PlaySettlementResult;
import com.meeny.domain.activity.pin.Split;
import com.meeny.domain.activity.pin.Transfer;
import com.meeny.domain.identity.Member;

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
    // 멤버 정보는 memberId → Member 맵으로 한 번에 주입. TransferDto/PinTransferDto 에서 수금자(to) 의
    // 표시용 닉네임뿐 아니라 계좌 정보를 함께 내려보내 프론트가 별도 조회 없이 "계좌 복사/공유" 를 만들 수 있게 한다.
    // 탈퇴자는 닉네임이 "(탈퇴한 사용자)" 로 마스킹되고 계좌 필드는 null 로 나간다.
    public static PlaySettlementResponse from(
            Long playId,
            LocalDateTime settledAt,
            PlaySettlementResult result,
            List<Pin> pins,
            List<PinTransferMark> marks,
            Map<Long, Member> members
    ) {
        Function<Long, String> nicknameOf = id -> {
            Member m = members.get(id);
            return m == null ? "" : m.getDisplayNickname();
        };
        Function<Long, MemberBankInfo> bankOf = id -> {
            Member m = members.get(id);
            return m == null ? MemberBankInfo.EMPTY : MemberBankInfo.from(m);
        };
        return new PlaySettlementResponse(
                playId,
                settledAt,
                result.totalAmount(),
                result.memberBalances().stream()
                        .map(b -> MemberBalanceDto.from(b, nicknameOf.apply(b.memberId())))
                        .toList(),
                result.transfers().stream()
                        .map(t -> TransferDto.from(
                                t,
                                nicknameOf.apply(t.fromMemberId()),
                                nicknameOf.apply(t.toMemberId()),
                                bankOf.apply(t.toMemberId())
                        ))
                        .toList(),
                buildPinTransfers(pins, marks, nicknameOf, bankOf)
        );
    }

    // 핀별 (fromMemberId → paidBy) 단위 송금 상태. paidBy 자신과 amount==0 split 은 제외.
    private static List<PinTransferDto> buildPinTransfers(
            List<Pin> pins,
            List<PinTransferMark> marks,
            Function<Long, String> nicknameOf,
            Function<Long, MemberBankInfo> bankOf
    ) {
        Map<String, PinTransferMark> markMap = marks.stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.getPinId() + ":" + m.getFromMemberId() + ":" + m.getToMemberId(),
                        m -> m
                ));
        return pins.stream()
                .flatMap(pin -> {
                    Long paidBy = pin.getSettlement().getPaidBy();
                    MemberBankInfo paidByBank = bankOf.apply(paidBy);
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
                                        receivedAt,
                                        paidByBank.bankCode(),
                                        paidByBank.accountNumber(),
                                        paidByBank.accountHolderName()
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
            long amount,
            String toBankCode,
            String toAccountNumber,
            String toAccountHolderName
    ) {
        public static TransferDto from(Transfer t, String fromNickname, String toNickname, MemberBankInfo toBank) {
            return new TransferDto(
                    t.fromMemberId(), fromNickname,
                    t.toMemberId(), toNickname,
                    t.amount(),
                    toBank.bankCode(), toBank.accountNumber(), toBank.accountHolderName()
            );
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
            LocalDateTime receivedAt,
            String toBankCode,
            String toAccountNumber,
            String toAccountHolderName
    ) {
    }

    // 수금자(paidBy) 계좌 스냅샷. 응답 시점의 값을 그대로 스냅샷으로 내려보낸다 (이후 사용자가 계좌를 바꿔도
    // 이 응답에는 반영되지 않음 — 어차피 다음 정산 조회에서 최신값을 다시 받게 됨).
    public record MemberBankInfo(String bankCode, String accountNumber, String accountHolderName) {
        static final MemberBankInfo EMPTY = new MemberBankInfo(null, null, null);

        static MemberBankInfo from(Member member) {
            if (member.isDeleted()) return EMPTY;
            return new MemberBankInfo(member.getBankCode(), member.getAccountNumber(), member.getAccountHolderName());
        }
    }
}
