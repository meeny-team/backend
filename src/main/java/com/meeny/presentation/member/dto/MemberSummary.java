package com.meeny.presentation.member.dto;

import com.meeny.domain.identity.Member;
import com.meeny.infrastructure.aws.S3UrlSigner;

// 크루/플레이 응답에 멤버를 표시할 때 쓰는 가벼운 요약. 닉네임/프로필/계좌를 같이 내려줘서 클라이언트가
// 멤버 ID 별로 별도 호출하지 않아도 정산 화면에서 계좌 복사/공유 UI 를 그릴 수 있게 한다.
// 탈퇴 회원은 닉네임을 마스킹하고 이미지/계좌 필드는 노출하지 않는다.
public record MemberSummary(
        Long id,
        String nickname,
        String profileImage,
        String bankCode,
        String accountNumber,
        String accountHolderName
) {

    public static MemberSummary from(Member member, S3UrlSigner imageSigner) {
        boolean deleted = member.isDeleted();
        return new MemberSummary(
                member.getId(),
                member.getDisplayNickname(),
                deleted ? null : imageSigner.sign(member.getProfileImage()),
                deleted ? null : member.getBankCode(),
                deleted ? null : member.getAccountNumber(),
                deleted ? null : member.getAccountHolderName()
        );
    }
}
