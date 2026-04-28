package com.meeny.domain.activity.pin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public record PlaySettlementResult(
        long totalAmount,
        List<MemberBalance> memberBalances,
        List<Transfer> transfers
) {

    public static PlaySettlementResult of(Set<Long> memberIds, List<Pin> pins) {
        Map<Long, Long> paid = new HashMap<>();
        Map<Long, Long> share = new HashMap<>();
        for (Long mid : memberIds) {
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

        List<MemberBalance> balances = memberIds.stream()
                .sorted()
                .map(mid -> {
                    long p = paid.getOrDefault(mid, 0L);
                    long s = share.getOrDefault(mid, 0L);
                    return new MemberBalance(mid, p, s, p - s);
                })
                .toList();

        return new PlaySettlementResult(total, balances, calculateTransfers(balances));
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
