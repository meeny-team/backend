package com.meeny.presentation.play.dto;

import com.meeny.domain.play.MemberBalance;
import com.meeny.domain.play.PlaySettlementResult;
import com.meeny.domain.play.Transfer;

import java.util.List;

public record PlaySettlementResponse(
        Long playId,
        long totalAmount,
        List<MemberBalanceDto> memberBalances,
        List<TransferDto> transfers
) {
    public static PlaySettlementResponse from(Long playId, PlaySettlementResult result) {
        return new PlaySettlementResponse(
                playId,
                result.totalAmount(),
                result.memberBalances().stream().map(MemberBalanceDto::from).toList(),
                result.transfers().stream().map(TransferDto::from).toList()
        );
    }

    public record MemberBalanceDto(
            Long memberId,
            long totalPaid,
            long totalShare,
            long balance
    ) {
        public static MemberBalanceDto from(MemberBalance b) {
            return new MemberBalanceDto(b.memberId(), b.totalPaid(), b.totalShare(), b.balance());
        }
    }

    public record TransferDto(
            Long fromMemberId,
            Long toMemberId,
            long amount
    ) {
        public static TransferDto from(Transfer t) {
            return new TransferDto(t.fromMemberId(), t.toMemberId(), t.amount());
        }
    }
}
