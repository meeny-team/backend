package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.log.ActivityLog;
import com.meeny.domain.activity.log.ActivityLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogJpaRepository
        extends JpaRepository<ActivityLog, Long>, ActivityLogRepository {

    List<ActivityLog> findByCrewIdOrderByCreatedAtDescIdDesc(Long crewId, org.springframework.data.domain.Pageable pageable);

    @Override
    default List<ActivityLog> findTopByCrewIdOrderByCreatedAtDesc(Long crewId, int limit) {
        return findByCrewIdOrderByCreatedAtDescIdDesc(crewId, PageRequest.of(0, limit));
    }
}
