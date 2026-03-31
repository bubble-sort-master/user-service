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
import com.innowise.userservice.specification.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;

  private static final String USERS_CACHE          = "users";
  private static final String USER_CARDS_CACHE     = "userCards";
  private static final String CARDS_CACHE          = "cards";
  private static final String CARDS_BY_USER_KEY    = "'cards::' + #userId";
  private static final String CARD_BY_ID_KEY       = "'byId::' + #cardId";
  private static final String CARD_BY_ID_FROM_RESULT = "'byId::' + #result.id";
  private static final String ALL_CARDS_KEY_PREFIX = "'allCards::'";
  private static final String ALL_CARDS_KEY_PATTERN =
          ALL_CARDS_KEY_PREFIX + " + #name + '::' + #surname + '::' + #pageable";

  private final PaymentCardRepository cardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper cardMapper;

  @Override
  @Transactional
  @Caching(evict = {
          @CacheEvict(value = USER_CARDS_CACHE, key = CARDS_BY_USER_KEY),
          @CacheEvict(value = USERS_CACHE, allEntries = true),
          @CacheEvict(value = CARDS_CACHE, allEntries = true)
          },
          put = @CachePut(value = CARDS_CACHE, key = CARD_BY_ID_FROM_RESULT)
  )
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
  @Cacheable(value = CARDS_CACHE, key = CARD_BY_ID_KEY)
  public CardShortDto getCardById(Long cardId) {
    PaymentCard card = cardRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException(cardId));
    return cardMapper.toShortDto(card);
  }

  @Override
  @Cacheable(value = USER_CARDS_CACHE, key = CARDS_BY_USER_KEY)
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
  @Cacheable(value = CARDS_CACHE, key = ALL_CARDS_KEY_PATTERN)
  public Page<CardShortDto> getAllCards(String name, String surname, Pageable pageable) {
    Specification<PaymentCard> spec = PaymentCardSpecifications.searchByUserNameAndSurname(name, surname);

    return cardRepository.findAll(spec, pageable)
            .map(cardMapper::toShortDto);
  }

  @Override
  @Transactional
  @Caching(evict = {
          @CacheEvict(value = USER_CARDS_CACHE, allEntries = true),
          @CacheEvict(value = USERS_CACHE, allEntries = true),
          @CacheEvict(value = CARDS_CACHE, allEntries = true)
          },
          put = @CachePut(value = CARDS_CACHE, key = CARD_BY_ID_KEY)
  )
  public CardShortDto updateCard(Long cardId, CardUpdateDto dto) {
    PaymentCard card = cardRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException(cardId));
    cardMapper.updateFromDto(dto, card);
    PaymentCard updated = cardRepository.save(card);

    return cardMapper.toShortDto(updated);
  }

  @Override
  @Transactional
  @Caching(evict = {
          @CacheEvict(value = USER_CARDS_CACHE, allEntries = true),
          @CacheEvict(value = CARDS_CACHE, key = CARD_BY_ID_KEY),
          @CacheEvict(value = USERS_CACHE, allEntries = true),
          @CacheEvict(value = CARDS_CACHE, allEntries = true)
  })
  public void changeCardActiveStatus(Long cardId, boolean active) {
    cardRepository.setActive(cardId, active);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isCardOwner(Long cardId, Long userId) {
    if (cardId == null || userId == null) {
      return false;
    }
    return cardRepository.existsByIdAndUserId(cardId, userId);
  }
}