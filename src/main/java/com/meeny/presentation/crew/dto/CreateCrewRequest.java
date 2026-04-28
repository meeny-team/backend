package com.meeny.presentation.crew.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCrewRequest(
        @NotBlank(message = "크루 이름은 필수입니다.")
        @Size(max = 50, message = "크루 이름은 50자 이하여야 합니다.")
        String name,

        String coverImage
) {}
