package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;

import java.util.List;

public interface PaymentCardService {

  CardShortDto createCard(Long userId, CardCreateDto dto);

  CardShortDto getCardById(Long cardId);

  List<CardShortDto> getCardsByUserId(Long userId);

  CardShortDto updateCard(Long cardId, CardUpdateDto dto);

  void changeCardActiveStatus(Long cardId, boolean active);
}