package com.loanplatform.collection.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveCollectionRequest {
    @NotBlank(message = "Resolution type is required")
    private String resolutionType;

    private String notes;
}
