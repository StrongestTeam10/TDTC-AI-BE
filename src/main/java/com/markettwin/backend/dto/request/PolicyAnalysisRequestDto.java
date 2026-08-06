package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PolicyAnalysisRequestDto(
        @NotBlank
        String policyText
) {
}
