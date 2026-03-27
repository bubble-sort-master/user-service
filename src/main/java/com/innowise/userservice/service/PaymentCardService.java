package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for payment card business logic.
 * Handles all operations related to creation, retrieval, updating and status management of payment cards.
 */
public interface PaymentCardService {

  /**
   * Creates a new payment card for the given user.
   *
   * @param userId the ID of the user who will own the card
   * @param dto the data needed to create the card
   * @return the created card
   */
  CardShortDto createCard(Long userId, CardCreateDto dto);

  /**
   * Retrieves a payment card by its ID.
   *
   * @param cardId the ID of the card
   * @return the payment card
   */
  CardShortDto getCardById(Long cardId);

  /**
   * Retrieves all payment cards belonging to a specific user.
   *
   * @param userId the ID of the user
   * @return list of payment cards
   */
  List<CardShortDto> getCardsByUserId(Long userId);

  /**
   * Retrieves all payment cards with optional filtering and pagination.
   *
   * @param name optional filter by user's first name
   * @param surname optional filter by user's last name
   * @param pageable pagination and sorting information
   * @return paginated list of payment cards
   */
  Page<CardShortDto> getAllCards(String name, String surname, Pageable pageable);

  /**
   * Updates an existing payment card.
   *
   * @param cardId the ID of the card to update
   * @param dto the updated card data
   * @return the updated card
   */
  CardShortDto updateCard(Long cardId, CardUpdateDto dto);

  /**
   * Changes the active status of a payment card.
   *
   * @param cardId the ID of the card
   * @param active the new active status
   */
  void changeCardActiveStatus(Long cardId, boolean active);

  /**
   * Checks whether the given user is the owner of the specified card.
   *
   * @param cardId the ID of the card
   * @param userId the ID of the user
   * @return true if the user is the owner, false otherwise
   */
  boolean isCardOwner(Long cardId, Long userId);
}