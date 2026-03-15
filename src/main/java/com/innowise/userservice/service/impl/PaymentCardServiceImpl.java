package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.CardCreateDto;
import com.innowise.userservice.dto.CardShortDto;
import com.innowise.userservice.dto.CardUpdateDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.MaximumCardsLimitExceededException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;

  private final PaymentCardRepository cardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper cardMapper;
  private final CacheManager cacheManager;

  @Override
  @Transactional
  @CacheEvict(value = "userCards", key = "'cards::' + #userId")
  public CardShortDto createCard(Long userId, CardCreateDto dto) {
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }

    long currentCount = cardRepository.countAllCardsByUserId(userId);
    if (currentCount >= MAX_CARDS_PER_USER) {
      throw new MaximumCardsLimitExceededException(MAX_CARDS_PER_USER);
    }

    User user = userRepository.getReferenceById(userId);

    PaymentCard card = cardMapper.toEntity(dto);
    card.setUser(user);
    card.setActive(true);

    PaymentCard saved = cardRepository.save(card);
    return cardMapper.toShortDto(saved);
  }

  @Override
  public CardShortDto getCardById(Long cardId) {
    PaymentCard card = cardRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException(cardId));
    return cardMapper.toShortDto(card);
  }

  @Override
  @Cacheable(value = "userCards", key = "'cards::' + #userId")
  public List<CardShortDto> getCardsByUserId(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }
    return cardRepository.findByUserId(userId)
            .stream()
            .map(cardMapper::toShortDto)
            .toList();
  }

  @Override
  @Transactional
  public CardShortDto updateCard(Long cardId, CardUpdateDto dto) {

    PaymentCard card = cardRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException(cardId));
    Long userId = card.getUser().getId();
    cardMapper.updateFromDto(dto, card);
    PaymentCard updated = cardRepository.save(card);

    evictUserCardsCache(userId);

    return cardMapper.toShortDto(updated);
  }

  @Override
  @Transactional
  public void changeCardActiveStatus(Long cardId, boolean active) {
    PaymentCard card = cardRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException(cardId));
    Long userId = card.getUser().getId();

    card.setActive(active);
    cardRepository.save(card);

    evictUserCardsCache(userId);
  }

  private void evictUserCardsCache(Long userId) {
    Cache cache = cacheManager.getCache("userCards");
    if (cache != null) {
      cache.evict("cards::" + userId);
    }
  }
}