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
        Member member = findActiveMember(memberId);
        return MemberProfileResponse.from(member);
    }

    // 프로필 수정: 닉네임, 프로필 이미지, 자기소개 변경
    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findActiveMember(memberId);
        member.updateProfile(request.nickname(), request.profileImage(), request.bio());
        return MemberProfileResponse.from(member);
    }

    // 회원 탈퇴: 과거 정산 외래키 무결성을 지키기 위해 소프트 딜리트, 토큰만 즉시 폐기
    @Transactional
    public void withdraw(Long memberId) {
        Member member = findActiveMember(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);
        member.withdraw();
    }

    // 탈퇴자도 외래키 참조 표시 등을 위해 조회 가능해야 하므로, 활성 회원이 필요한 경우에만 별도 분기
    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isDeleted()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
