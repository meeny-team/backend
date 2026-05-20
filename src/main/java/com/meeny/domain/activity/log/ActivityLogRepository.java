package com.meeny.domain.activity.log;

import java.util.List;

public interface ActivityLogRepository {
    ActivityLog save(ActivityLog log);
    // 최신순 N 개. 크루 피드의 기본 조회.
    List<ActivityLog> findTopByCrewIdOrderByCreatedAtDesc(Long crewId, int limit);
}
