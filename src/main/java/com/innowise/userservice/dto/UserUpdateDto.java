package com.innowise.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserUpdateDto(
        @Size(min = 2, max = 100) String name,

        @Size(min = 2, max = 100) String surname,

        @Past LocalDate birthDate,

        @Email String email
) {}
