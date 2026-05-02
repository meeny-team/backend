package com.meeny.domain.activity.pin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public record PlaySettlementResult(
        long totalAmount,
        List<MemberBalance> memberBalances,
        List<Transfer> transfers
) {

    // Play의 현재 멤버뿐 아니라 과거 핀에 결제자/분담자로 등장한 모든 멤버를 합산 대상에 포함해야
    // 탈퇴/제외된 멤버의 채권·채무가 사라지지 않음 (정산 표시는 그대로 N명 기준).
    public static PlaySettlementResult of(Set<Long> memberIds, List<Pin> pins) {
        Set<Long> allMemberIds = new HashSet<>(memberIds);
        for (Pin pin : pins) {
            allMemberIds.add(pin.getSettlement().getPaidBy());
            for (Split split : pin.getSplits()) {
                allMemberIds.add(split.getUserId());
            }
        }

        Map<Long, Long> paid = new HashMap<>();
        Map<Long, Long> share = new HashMap<>();
        for (Long mid : allMemberIds) {
            paid.put(mid, 0L);
            share.put(mid, 0L);
        }

        long total = 0L;
        for (Pin pin : pins) {
            total += pin.getAmount();
            paid.merge(pin.getSettlement().getPaidBy(), pin.getAmount(), Long::sum);
            for (Split split : pin.getSplits()) {
                share.merge(split.getUserId(), split.getAmount(), Long::sum);
            }
        }

        List<MemberBalance> balances = allMemberIds.stream()
                .sorted()
                .map(mid -> {
                    long p = paid.get(mid);
                    long s = share.get(mid);
                    return new MemberBalance(mid, p, s, p - s);
                })
                .toList();

        return new PlaySettlementResult(total, balances, calculateTransfers(balances));
    }

    // 멤버 한 명의 미정산 잔액 (양수=받을 돈, 음수=낼 돈, 0=정산 완료). Crew 탈퇴/Play 제외 차단 가드에 사용.
    public static long memberBalance(Long memberId, List<Pin> pins) {
        long paid = 0L;
        long share = 0L;
        for (Pin pin : pins) {
            if (pin.getSettlement().getPaidBy().equals(memberId)) {
                paid += pin.getAmount();
            }
            for (Split split : pin.getSplits()) {
                if (split.getUserId().equals(memberId)) {
                    share += split.getAmount();
                }
            }
        }
        return paid - share;
    }

    private static List<Transfer> calculateTransfers(List<MemberBalance> balances) {
        Comparator<long[]> byAmountDescIdAsc = Comparator
                .<long[]>comparingLong(a -> -a[1])
                .thenComparingLong(a -> a[0]);

        PriorityQueue<long[]> creditors = new PriorityQueue<>(byAmountDescIdAsc);
        PriorityQueue<long[]> debtors = new PriorityQueue<>(byAmountDescIdAsc);

        for (MemberBalance b : balances) {
            if (b.balance() > 0) creditors.offer(new long[]{b.memberId(), b.balance()});
            else if (b.balance() < 0) debtors.offer(new long[]{b.memberId(), -b.balance()});
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] c = creditors.poll();
            long[] d = debtors.poll();
            long amount = Math.min(c[1], d[1]);
            transfers.add(new Transfer(d[0], c[0], amount));
            if (c[1] > amount) creditors.offer(new long[]{c[0], c[1] - amount});
            if (d[1] > amount) debtors.offer(new long[]{d[0], d[1] - amount});
        }
        return transfers;
    }
}
