package com.innowise.userservice.dto;

public record UserShortDto(
        Long id,
        String name,
        String surname,
        String email,
        Boolean active,
        int cardsCount
) {}
