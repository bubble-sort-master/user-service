package com.innowise.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record CardActiveStatusDto(
        @NotNull(message = "Field 'active' is required")
        Boolean active
) {}