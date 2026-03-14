package com.innowise.userservice.dto;

import java.time.LocalDate;

public record CardShortDto(
        Long id,
        String numberMasked,
        String holder,
        LocalDate expirationDate,
        Boolean active
) {}
