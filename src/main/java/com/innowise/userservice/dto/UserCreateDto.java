package com.innowise.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserCreateDto(
        @NotBlank @Size(min = 2, max = 100) String name,

        @NotBlank @Size(min = 2, max = 100) String surname,

        @Past LocalDate birthDate,

        @NotBlank @Email String email
) {}