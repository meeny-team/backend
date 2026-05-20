package com.meeny.application.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.activity.log.ActivityLog;
import com.meeny.domain.activity.log.ActivityLogRepository;
import com.meeny.domain.activity.log.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    // 호출자(도메인 service) 의 @Transactional 안에서 같이 커밋된다 — mutation 이 롤백되면 활동 로그도 함께 롤백.
    public void record(Long crewId, Long actorMemberId, ActivityType type, Object payload) {
        String json = payload == null ? null : serialize(payload);
        activityLogRepository.save(ActivityLog.of(crewId, actorMemberId, type, json));
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> fetchRecent(Long crewId, int limit) {
        return activityLogRepository.findTopByCrewIdOrderByCreatedAtDesc(crewId, limit);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize activity payload", e);
        }
    }
}
