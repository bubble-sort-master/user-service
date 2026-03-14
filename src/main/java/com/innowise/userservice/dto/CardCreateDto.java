package com.innowise.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CardCreateDto(
        @NotBlank @Size(min = 13, max = 19)
        @Pattern(regexp = "^\\d(?:\\s*\\d){12,18}\\s*$")
        String number,

        @NotBlank @Size(max = 200)
        String holder,

        @NotNull @Future
        LocalDate expirationDate
) {}
