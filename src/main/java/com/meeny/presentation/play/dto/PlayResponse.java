package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayType;

import java.time.LocalDateTime;
import java.util.List;

public record PlayResponse(
        Long id,
        Long crewId,
        String title,
        PlayType type,
        DateRangeDto dateRange,
        List<Long> memberIds,
        List<String> regions,
        List<String> tags,
        String coverImage,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static PlayResponse from(Play play) {
        return new PlayResponse(
                play.getId(),
                play.getCrewId(),
                play.getTitle(),
                play.getType(),
                DateRangeDto.from(play.getDateRange()),
                play.getMemberIds().stream().toList(),
                play.getRegions(),
                play.getTags(),
                play.getCoverImage(),
                play.getCreatedBy(),
                play.getCreatedAt()
        );
    }
}
