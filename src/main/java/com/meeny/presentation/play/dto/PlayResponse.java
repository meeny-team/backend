package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayType;
import com.meeny.domain.identity.Member;
import com.meeny.infrastructure.aws.S3UrlSigner;
import com.meeny.presentation.member.dto.MemberSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PlayResponse(
        Long id,
        Long crewId,
        String title,
        PlayType type,
        DateRangeDto dateRange,
        List<MemberSummary> members,
        List<String> regions,
        List<String> tags,
        String coverImage,
        Long createdBy,
        LocalDateTime createdAt
) {
    // 호출자가 멤버 엔티티를 미리 조회해 넘겨준다 — Crew/Play 응답에 닉네임을 같이 내려주기 위함.
    public static PlayResponse from(Play play, List<Member> members, S3UrlSigner imageSigner) {
        Map<Long, Member> byId = members.stream()
                .collect(java.util.stream.Collectors.toMap(Member::getId, m -> m));
        List<MemberSummary> summaries = play.getMemberIds().stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(m -> MemberSummary.from(m, imageSigner))
                .toList();
        return new PlayResponse(
                play.getId(),
                play.getCrewId(),
                play.getTitle(),
                play.getType(),
                DateRangeDto.from(play.getDateRange()),
                summaries,
                play.getRegions(),
                play.getTags(),
                imageSigner.sign(play.getCoverImage()),
                play.getCreatedBy(),
                play.getCreatedAt()
        );
    }
}
