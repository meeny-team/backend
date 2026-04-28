package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.play.PlayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record UpdatePlayRequest(
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        PlayType type,

        @Valid
        DateRangeDto dateRange,

        Set<Long> memberIds,

        List<String> regions,

        List<String> tags,

        String coverImage
) {}
