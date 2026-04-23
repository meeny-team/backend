package com.meeny.member.application;

import com.meeny.global.exception.BusinessException;
import com.meeny.global.exception.ErrorCode;
import com.meeny.member.application.dto.MemberProfileResponse;
import com.meeny.member.domain.Member;
import com.meeny.member.domain.MemberRepository;
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
