package com.meeny.presentation.crew.dto;

import com.meeny.domain.crew.Crew;

import java.time.LocalDateTime;
import java.util.List;

public record CrewResponse(
        Long id,
        String name,
        String inviteCode,
        String coverImage,
        Long createdBy,
        LocalDateTime createdAt,
        List<Long> memberIds
) {
    public static CrewResponse from(Crew crew) {
        return new CrewResponse(
                crew.getId(),
                crew.getName(),
                crew.getInviteCode().getValue(),
                crew.getCoverImage(),
                crew.getCreatedBy(),
                crew.getCreatedAt(),
                crew.getMemberIds().stream().toList()
        );
    }
}
