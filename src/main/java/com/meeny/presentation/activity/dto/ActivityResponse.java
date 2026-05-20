package com.meeny.presentation.activity.dto;

import com.meeny.domain.activity.log.ActivityLog;
import com.meeny.domain.activity.log.ActivityType;
import com.meeny.domain.identity.Member;

import java.time.LocalDateTime;
import java.util.Map;

public record ActivityResponse(
        Long id,
        ActivityType type,
        ActorDto actor,
        Map<String, Object> payload,
        LocalDateTime createdAt
) {
    // actor 가 null 인 경우 = 시스템 발생 활동. 탈퇴자는 "(탈퇴한 사용자)" 마스킹.
    public record ActorDto(Long memberId, String nickname, String profileImage) {}

    public static ActivityResponse from(ActivityLog log, Member actor, Map<String, Object> payload) {
        ActorDto actorDto = log.getActorMemberId() == null
                ? null
                : actor == null
                    ? new ActorDto(log.getActorMemberId(), "(탈퇴한 사용자)", null)
                    : new ActorDto(actor.getId(), actor.getDisplayNickname(), actor.getProfileImage());
        return new ActivityResponse(log.getId(), log.getType(), actorDto, payload, log.getCreatedAt());
    }
}
