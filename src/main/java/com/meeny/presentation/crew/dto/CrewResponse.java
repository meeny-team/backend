package com.meeny.presentation.crew.dto;

import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.identity.Member;
import com.meeny.infrastructure.aws.S3UrlSigner;
import com.meeny.presentation.member.dto.MemberSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CrewResponse(
        Long id,
        String name,
        String inviteCode,
        String coverImage,
        Long createdBy,
        LocalDateTime createdAt,
        List<MemberSummary> members
) {
    // members: 닉네임/프로필이 같이 들어가야 하므로, 호출자가 멤버 엔티티를 미리 조회해 넘겨준다.
    // memberId 순서는 Crew 의 memberIds 순서를 따른다 (탈퇴자도 ID 만 남아 있으면 마스킹된 채로 포함).
    public static CrewResponse from(Crew crew, List<Member> members, S3UrlSigner imageSigner) {
        Map<Long, Member> byId = members.stream()
                .collect(java.util.stream.Collectors.toMap(Member::getId, m -> m));
        List<MemberSummary> summaries = crew.getMemberIds().stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(m -> MemberSummary.from(m, imageSigner))
                .toList();
        return new CrewResponse(
                crew.getId(),
                crew.getName(),
                crew.getInviteCode().getValue(),
                imageSigner.sign(crew.getCoverImage()),
                crew.getCreatedBy(),
                crew.getCreatedAt(),
                summaries
        );
    }
}
