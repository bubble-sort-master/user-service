package com.innowise.userservice.controller;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * REST controller for managing payment cards.
 * Provides endpoints to create, retrieve, update and manage status of payment cards.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentCardController {

  private final PaymentCardService cardService;


  private static final String IS_ADMIN = "hasRole('ADMIN')";

  private static final String IS_USER_OWN_RESOURCE =
          "hasRole('USER') and #userId.toString() == authentication.name";

  private static final String IS_ADMIN_OR_CARD_OWNER =
          "hasRole('ADMIN') or @paymentCardService.isCardOwner(#cardId, " +
                  "T(java.lang.Long).valueOf(authentication.name))";

  private static final String IS_ADMIN_OR_USER_OWN_RESOURCE =
          IS_ADMIN + " or " + IS_USER_OWN_RESOURCE;

  /**
   * Creates a new payment card for the specified user.
   *
   * @param userId the ID of the user who will own the card
   * @param dto the data required to create the card
   * @param uriBuilder used to generate the Location header for the created resource
   * @return the created card with HTTP 201 Created status
   */
  @PostMapping("/users/{userId}/cards")
  @PreAuthorize(IS_ADMIN_OR_USER_OWN_RESOURCE)
  public ResponseEntity<CardShortDto> createCard(
          @PathVariable Long userId,
          @Valid @RequestBody CardCreateDto dto,
          UriComponentsBuilder uriBuilder) {

    CardShortDto created = cardService.createCard(userId, dto);

    var uri = uriBuilder
            .path("/api/cards/{id}")
            .buildAndExpand(created.id())
            .toUri();

    return ResponseEntity.created(uri).body(created);
  }

  /**
   * Retrieves a payment card by its ID.
   * Administrators can view any card, regular users can only view their own cards.
   *
   * @param cardId the ID of the card to retrieve
   * @return the payment card
   */
  @GetMapping("/cards/{cardId}")
  @PreAuthorize(IS_ADMIN_OR_CARD_OWNER)
  public ResponseEntity<CardShortDto> getCardById(@PathVariable Long cardId) {
    return ResponseEntity.ok(cardService.getCardById(cardId));
  }

  /**
   * Retrieves all payment cards belonging to a specific user.
   * Administrators can view any user's cards, users can only view their own.
   *
   * @param userId the ID of the user
   * @return list of the user's payment cards
   */
  @GetMapping("/users/{userId}/cards")
  @PreAuthorize(IS_ADMIN_OR_USER_OWN_RESOURCE)
  public ResponseEntity<List<CardShortDto>> getCardsByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.getCardsByUserId(userId));
  }

  /**
   * Retrieves all payment cards with optional filtering by name and surname and pagination.
   * Only accessible by administrators.
   *
   * @param name optional filter by user's first name
   * @param surname optional filter by user's last name
   * @param pageable pagination and sorting information
   * @return paginated list of payment cards
   */
  @GetMapping("/cards")
  @PreAuthorize(IS_ADMIN)
  public ResponseEntity<Page<CardShortDto>> getAllCards(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {

    Page<CardShortDto> page = cardService.getAllCards(name, surname, pageable);
    return ResponseEntity.ok(page);
  }

  /**
   * Updates an existing payment card.
   * Administrators can update any card, regular users can only update their own cards.
   *
   * @param cardId the ID of the card to update
   * @param dto the updated card data
   * @return the updated card
   */
  @PutMapping("/cards/{cardId}")
  @PreAuthorize(IS_ADMIN_OR_CARD_OWNER)
  public ResponseEntity<CardShortDto> updateCard(
          @PathVariable Long cardId,
          @Valid @RequestBody CardUpdateDto dto) {

    return ResponseEntity.ok(cardService.updateCard(cardId, dto));
  }

  /**
   * Changes the active/inactive status of a payment card.
   * Administrators can change status of any card, users can only change their own card.
   *
   * @param cardId the ID of the card
   * @param statusDto contains the new active status
   */
  @PatchMapping("/cards/{cardId}")
  @PreAuthorize(IS_ADMIN_OR_CARD_OWNER)
  public ResponseEntity<Void> changeCardActiveStatus(
          @PathVariable Long cardId,
          @Valid @RequestBody CardActiveStatusDto statusDto) {

    cardService.changeCardActiveStatus(cardId, statusDto.active());
    return ResponseEntity.noContent().build();
  }
}