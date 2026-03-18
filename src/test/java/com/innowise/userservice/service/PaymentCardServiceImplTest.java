package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.MaximumCardsLimitExceededException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.impl.PaymentCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

  @Mock private PaymentCardRepository cardRepository;
  @Mock private UserRepository userRepository;
  @Mock private PaymentCardMapper cardMapper;

  @InjectMocks private PaymentCardServiceImpl cardService;

  private CardCreateDto createDto;
  private CardUpdateDto updateDto;
  private User user;
  private PaymentCard cardEntity;
  private CardShortDto shortDto;

  @BeforeEach
  void setUp() {
    createDto = new CardCreateDto("4111111111111111", "John Doe", LocalDate.of(2030, 12, 31));
    updateDto = new CardUpdateDto("John Doe Updated", LocalDate.of(2030, 12, 31), false);

    user = new User();
    user.setId(1L);

    cardEntity = new PaymentCard();
    cardEntity.setId(10L);
    cardEntity.setNumber("4111111111111111");
    cardEntity.setHolder("John Doe");
    cardEntity.setExpirationDate(LocalDate.of(2030, 12, 31));
    cardEntity.setActive(true);
    cardEntity.setUser(user);

    shortDto = new CardShortDto(10L, "4111************1111", "John Doe",
            LocalDate.of(2030, 12, 31), true);
  }

  @Test
  void createCard_success() {
    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.countAllCardsByUserId(1L)).thenReturn(3L);
    when(userRepository.getReferenceById(1L)).thenReturn(user);
    when(cardMapper.toEntity(createDto)).thenReturn(cardEntity);
    when(cardRepository.save(any(PaymentCard.class))).thenReturn(cardEntity);
    when(cardMapper.toShortDto(cardEntity)).thenReturn(shortDto);

    CardShortDto result = cardService.createCard(1L, createDto);

    assertThat(result).isEqualTo(shortDto);
  }

  @Test
  void createCard_userNotFound_throwsUserNotFoundException() {
    when(userRepository.existsById(999L)).thenReturn(false);

    assertThatThrownBy(() -> cardService.createCard(999L, createDto))
            .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void createCard_maxCardsExceeded_throwsMaximumCardsLimitExceededException() {
    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.countAllCardsByUserId(1L)).thenReturn(5L);

    assertThatThrownBy(() -> cardService.createCard(1L, createDto))
            .isInstanceOf(MaximumCardsLimitExceededException.class);
  }

  @Test
  void getCardById_success() {
    when(cardRepository.findById(10L)).thenReturn(Optional.of(cardEntity));
    when(cardMapper.toShortDto(cardEntity)).thenReturn(shortDto);

    CardShortDto result = cardService.getCardById(10L);

    assertThat(result).isEqualTo(shortDto);
  }

  @Test
  void getCardById_notFound_throwsCardNotFoundException() {
    when(cardRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.getCardById(999L))
            .isInstanceOf(CardNotFoundException.class);
  }

  @Test
  void getCardsByUserId_success() {
    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.findByUserId(1L)).thenReturn(List.of(cardEntity));
    when(cardMapper.toShortDto(any(PaymentCard.class))).thenReturn(shortDto);

    List<CardShortDto> result = cardService.getCardsByUserId(1L);

    assertThat(result).hasSize(1);
  }

  @Test
  void getCardsByUserId_userNotFound_throwsUserNotFoundException() {
    when(userRepository.existsById(999L)).thenReturn(false);

    assertThatThrownBy(() -> cardService.getCardsByUserId(999L))
            .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updateCard_success() {
    when(cardRepository.findById(10L)).thenReturn(Optional.of(cardEntity));
    when(cardRepository.save(any(PaymentCard.class))).thenReturn(cardEntity);
    when(cardMapper.toShortDto(cardEntity)).thenReturn(shortDto);

    CardShortDto result = cardService.updateCard(10L, updateDto);

    assertThat(result).isEqualTo(shortDto);
    verify(cardMapper).updateFromDto(updateDto, cardEntity);
  }

  @Test
  void updateCard_notFound_throwsCardNotFoundException() {
    when(cardRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.updateCard(999L, updateDto))
            .isInstanceOf(CardNotFoundException.class);
  }

  @Test
  void changeCardActiveStatus_success() {
    when(cardRepository.findById(10L)).thenReturn(Optional.of(cardEntity));

    cardService.changeCardActiveStatus(10L, false);

    verify(cardRepository).save(cardEntity);
    assertThat(cardEntity.getActive()).isFalse();
  }

  @Test
  void changeCardActiveStatus_notFound_throwsCardNotFoundException() {
    when(cardRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.changeCardActiveStatus(999L, true))
            .isInstanceOf(CardNotFoundException.class);
  }
}