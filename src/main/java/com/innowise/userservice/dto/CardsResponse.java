package com.innowise.userservice.dto;

import java.io.Serializable;
import java.util.List;

public record CardsResponse(List<CardShortDto> content) implements Serializable { }