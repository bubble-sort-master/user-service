package com.innowise.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record UserActiveStatusDto(
        @NotNull(message = "Field 'active' is required")
        Boolean active
) {}