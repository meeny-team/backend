package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.pin.MemberBalance;
import com.meeny.domain.activity.pin.PlaySettlementResult;
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
        List<TransferDto> transfers
) {
    // 닉네임은 memberId → displayName 맵으로 한 번에 주입; 탈퇴자는 "(탈퇴한 사용자)"로 표시되도록 호출 측에서 미리 변환
    public static PlaySettlementResponse from(Long playId, LocalDateTime settledAt, PlaySettlementResult result, Map<Long, String> nicknames) {
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
                        .toList()
        );
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
}
