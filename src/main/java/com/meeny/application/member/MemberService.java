package com.meeny.application.member;

import com.meeny.presentation.member.dto.MemberProfileResponse;
import com.meeny.presentation.member.dto.UpdateProfileRequest;
import com.meeny.domain.auth.RefreshTokenRepository;
import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 내 프로필 조회
    public MemberProfileResponse getProfile(Long memberId) {
        Member member = findMember(memberId);
        return MemberProfileResponse.from(member);
    }

    // 프로필 수정: 닉네임, 프로필 이미지, 자기소개 변경
    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findMember(memberId);
        member.updateProfile(request.nickname(), request.profileImage(), request.bio());
        return MemberProfileResponse.from(member);
    }

    // 회원 탈퇴: 리프레시 토큰을 모두 삭제하고 회원 정보를 제거
    @Transactional
    public void withdraw(Long memberId) {
        Member member = findMember(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);
        memberRepository.delete(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
