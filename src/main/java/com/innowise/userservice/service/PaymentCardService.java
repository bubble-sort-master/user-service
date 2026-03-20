package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentCardService {

  CardShortDto createCard(Long userId, CardCreateDto dto);

  CardShortDto getCardById(Long cardId);

  List<CardShortDto> getCardsByUserId(Long userId);

  Page<CardShortDto> getAllCards(String name, String surname, Pageable pageable);

  CardShortDto updateCard(Long cardId, CardUpdateDto dto);

  void changeCardActiveStatus(Long cardId, boolean active);
}