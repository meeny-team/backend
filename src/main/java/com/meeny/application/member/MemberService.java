package com.meeny.application.member;

import com.meeny.presentation.member.dto.MemberProfileResponse;
import com.meeny.presentation.member.dto.UpdateProfileRequest;
import com.meeny.domain.auth.RefreshTokenRepository;
import com.meeny.domain.member.Member;
import com.meeny.domain.member.MemberRepository;
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

    public MemberProfileResponse getProfile(Long memberId) {
        Member member = findMember(memberId);
        return MemberProfileResponse.from(member);
    }

    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findMember(memberId);
        member.updateProfile(request.nickname(), request.profileImage(), request.bio());
        return MemberProfileResponse.from(member);
    }

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
