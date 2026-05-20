package com.meeny.domain.activity.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 크루 단위 활동 피드 항목. mutation 이 일어날 때마다 한 row 적재.
// payload 는 type 별로 다른 구조의 JSON 문자열 — 프론트 렌더 시 type 기준으로 파싱.
@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "ix_activity_logs_crew_created", columnList = "crew_id, created_at"),
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    // 행위자. 시스템 발생 활동 (예: 자동 처리) 이면 null 가능.
    @Column(name = "actor_member_id")
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ActivityType type;

    // type 별 추가 컨텍스트. JSON string. 예: {"playId":1,"pinTitle":"스타벅스","amount":12000}
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ActivityLog(Long crewId, Long actorMemberId, ActivityType type, String payload) {
        this.crewId = crewId;
        this.actorMemberId = actorMemberId;
        this.type = type;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public static ActivityLog of(Long crewId, Long actorMemberId, ActivityType type, String payload) {
        return new ActivityLog(crewId, actorMemberId, type, payload);
    }
}
