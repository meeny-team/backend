package com.meeny.application.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.log.ActivityLog;
import com.meeny.domain.activity.log.ActivityLogRepository;
import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.presentation.activity.dto.ActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityQueryService {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final CrewRepository crewRepository;
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    // 크루 멤버만 조회 가능. 최신 N 건 반환. actor 닉네임은 한 번에 join, 탈퇴자는 마스킹.
    public List<ActivityResponse> fetchByCrew(Long crewId, Long callerId, int limit) {
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(callerId);

        List<ActivityLog> logs = activityLogRepository.findTopByCrewIdOrderByCreatedAtDesc(crewId, limit);

        Set<Long> actorIds = logs.stream()
                .map(ActivityLog::getActorMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Member> actorMap = memberRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return logs.stream()
                .map(log -> ActivityResponse.from(
                        log,
                        log.getActorMemberId() == null ? null : actorMap.get(log.getActorMemberId()),
                        deserialize(log.getPayload())))
                .toList();
    }

    private Map<String, Object> deserialize(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            return objectMapper.readValue(payload, PAYLOAD_TYPE);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
