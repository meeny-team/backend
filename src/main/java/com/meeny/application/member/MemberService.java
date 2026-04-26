package com.meeny.application.member;

import com.meeny.presentation.member.dto.MemberProfileResponse;
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

    public MemberProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return new MemberProfileResponse(member.getId(), member.getEmail(), member.getNickname());
    }
}
