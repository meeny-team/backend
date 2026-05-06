package com.meeny.presentation.member.dto;

import com.meeny.domain.identity.Member;

// 크루/플레이 응답에 멤버를 표시할 때 쓰는 가벼운 요약. 닉네임/프로필을 같이 내려줘서 클라이언트가
// 멤버 ID 별로 별도 호출하지 않아도 화면에 이름을 표시할 수 있게 한다.
// 탈퇴 회원은 getDisplayNickname() 으로 "(탈퇴한 사용자)" 마스킹.
public record MemberSummary(Long id, String nickname, String profileImage) {

    public static MemberSummary from(Member member) {
        return new MemberSummary(
                member.getId(),
                member.getDisplayNickname(),
                member.isDeleted() ? null : member.getProfileImage()
        );
    }
}
