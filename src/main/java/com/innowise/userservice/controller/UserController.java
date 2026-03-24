package com.innowise.userservice.controller;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserShortDto> createUser(
          @Valid @RequestBody UserCreateDto dto,
          UriComponentsBuilder uriBuilder) {

    UserShortDto created = userService.createUser(dto);

    var uri = uriBuilder
            .path("/api/users/{id}")
            .buildAndExpand(created.id())
            .toUri();

    return ResponseEntity.created(uri).body(created);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserWithCardsDto> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @GetMapping
  public ResponseEntity<Page<UserWithCardsDto>> getAllUsers(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {

    Page<UserWithCardsDto> page = userService.getAllUsers(name, surname, pageable);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/with-cards")
  public ResponseEntity<Page<UserWithCardsDto>> getAllUsersWithCards(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {

    Page<UserWithCardsDto> page = userService.getAllUsersWithCards(name, surname, pageable);
    return ResponseEntity.ok(page);
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserShortDto> updateUser(
          @PathVariable Long id,
          @Valid @RequestBody UserUpdateDto dto) {

    return ResponseEntity.ok(userService.updateUser(id, dto));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> changeUserActiveStatus(
          @PathVariable Long id,
          @Valid @RequestBody UserActiveStatusDto statusDto) {

    userService.changeUserActiveStatus(id, statusDto.active());
    return ResponseEntity.noContent().build();
  }
}