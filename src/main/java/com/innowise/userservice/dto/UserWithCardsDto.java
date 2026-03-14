package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.util.List;

public record UserWithCardsDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active,
        List<CardShortDto> cards
) {}
