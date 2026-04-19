package com.innowise.userservice.controller;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.service.UserService;
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
 * REST controller for managing users.
 * Provides CRUD operations and active status management for user entities.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  private static final String IS_ADMIN = "hasRole('ADMIN')";
  private static final String IS_USER_OWNER = "hasRole('USER') and #id.toString() == authentication.name";
  private static final String IS_ADMIN_OR_USER_OWNER = IS_ADMIN + " or " + IS_USER_OWNER;

  /**
   * Creates a new user in the system.
   * Only administrators are allowed to create users.
   *
   * @param dto the user creation data
   * @param uriBuilder used to build the Location header
   * @return the created user with HTTP 201 Created status
   */
  @PostMapping
  @PreAuthorize(IS_ADMIN)
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

  /**
   * Retrieves a user by ID along with their payment cards.
   * Administrators can retrieve any user, regular users can only retrieve their own profile.
   *
   * @param id the ID of the user
   * @return the user with payment cards
   */
  @GetMapping("/{id}")
  @PreAuthorize(IS_ADMIN_OR_USER_OWNER)
  public ResponseEntity<UserWithCardsDto> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  /**
   * Retrieves a user by email.
   * <p>Admins or the user themselves can access.
   *
   * @param email user email
   * @return user with payment cards
   */
  @GetMapping("/by-email/{email}")
  @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isOwnerByEmail(#email, authentication)")
  public ResponseEntity<UserWithCardsDto> getUserByEmail(@PathVariable String email) {
    return ResponseEntity.ok(userService.getUserByEmail(email));
  }

  /**
   * Retrieves all users with optional filtering by name and surname and pagination.
   * Only accessible by administrators.
   *
   * @param name optional filter by first name
   * @param surname optional filter by last name
   * @param pageable pagination and sorting information
   * @return paginated list of users
   */
  @GetMapping
  @PreAuthorize(IS_ADMIN)
  public ResponseEntity<Page<UserWithCardsDto>> getAllUsers(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {

    Page<UserWithCardsDto> page = userService.getAllUsers(name, surname, pageable);
    return ResponseEntity.ok(page);
  }

  /**
   * Updates user information.
   * Administrators can update any user, regular users can only update their own profile.
   *
   * @param id the ID of the user to update
   * @param dto the updated user data
   * @return the updated user
   */
  @PutMapping("/{id}")
  @PreAuthorize(IS_ADMIN_OR_USER_OWNER)
  public ResponseEntity<UserShortDto> updateUser(
          @PathVariable Long id,
          @Valid @RequestBody UserUpdateDto dto) {

    return ResponseEntity.ok(userService.updateUser(id, dto));
  }

  /**
   * Changes the active/inactive status of a user.
   * Administrators can change any user's status, regular users can only change their own.
   *
   * @param id the ID of the user
   * @param statusDto contains the new active status
   */
  @PatchMapping("/{id}")
  @PreAuthorize(IS_ADMIN_OR_USER_OWNER)
  public ResponseEntity<Void> changeUserActiveStatus(
          @PathVariable Long id,
          @Valid @RequestBody UserActiveStatusDto statusDto) {

    userService.changeUserActiveStatus(id, statusDto.active());
    return ResponseEntity.noContent().build();
  }

  /**
   * Retrieves multiple users by their IDs.
   * <p>Admin only.
   *
   * @param ids list of user IDs
   * @return list of users with payment cards
   */
  @GetMapping("/bulk")
  @PreAuthorize(IS_ADMIN)
  public ResponseEntity<List<UserWithCardsDto>> getUsersByIds(
          @RequestParam("ids") List<Long> ids) {

    return ResponseEntity.ok(userService.getUsersByIds(ids));
  }
  /**
   * Soft Delete user by its ID.
   *
   * @param id identifier of the user.
   */
  @DeleteMapping("/{id}")
  @PreAuthorize(IS_ADMIN_OR_USER_OWNER)
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.changeUserActiveStatus(id, false);
    return ResponseEntity.noContent().build();
  }
}