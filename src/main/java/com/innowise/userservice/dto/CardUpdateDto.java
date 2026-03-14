package com.innowise.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CardUpdateDto(
        @Size(max = 200, message = "Holder name must not exceed 200 characters")
        String holder,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate,

        Boolean active

) {
}