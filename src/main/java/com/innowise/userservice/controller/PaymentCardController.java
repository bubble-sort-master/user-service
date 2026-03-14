package com.innowise.userservice.controller;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentCardController {

  private final PaymentCardService cardService;

  @PostMapping("/users/{userId}/cards")
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

  @GetMapping("/cards/{cardId}")
  public ResponseEntity<CardShortDto> getCardById(@PathVariable Long cardId) {
    return ResponseEntity.ok(cardService.getCardById(cardId));
  }

  @GetMapping("/users/{userId}/cards")
  public ResponseEntity<List<CardShortDto>> getCardsByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.getCardsByUserId(userId));
  }

  @PutMapping("/cards/{cardId}")
  public ResponseEntity<CardShortDto> updateCard(
          @PathVariable Long cardId,
          @Valid @RequestBody CardUpdateDto dto) {

    return ResponseEntity.ok(cardService.updateCard(cardId, dto));
  }

  @PatchMapping("/cards/{cardId}/active")
  public ResponseEntity<Void> changeCardActiveStatus(
          @PathVariable Long cardId,
          @RequestParam boolean active) {

    cardService.changeCardActiveStatus(cardId, active);
    return ResponseEntity.noContent().build();
  }
}