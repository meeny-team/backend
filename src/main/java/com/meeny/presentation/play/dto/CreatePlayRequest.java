package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.play.PlayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record CreatePlayRequest(
        @NotNull(message = "크루 ID는 필수입니다.")
        Long crewId,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "타입은 필수입니다.")
        PlayType type,

        @Valid
        @NotNull(message = "기간은 필수입니다.")
        DateRangeDto dateRange,

        Set<Long> memberIds,

        List<String> regions,

        List<String> tags,

        String coverImage
) {}
