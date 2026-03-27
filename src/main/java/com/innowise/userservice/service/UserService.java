package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for user business logic.
 * Handles user creation, retrieval, updating and active status management.
 */
public interface UserService {

  /**
   * Creates a new user in the system.
   *
   * @param dto the user creation data
   * @return the created user
   */
  UserShortDto createUser(UserCreateDto dto);

  /**
   * Retrieves a user by ID including their payment cards.
   *
   * @param id the ID of the user
   * @return the user with payment cards
   */
  UserWithCardsDto getUserById(Long id);

  /**
   * Retrieves all users with optional filtering and pagination.
   *
   * @param name optional filter by first name
   * @param surname optional filter by last name
   * @param pageable pagination information
   * @return paginated list of users
   */
  Page<UserWithCardsDto> getAllUsers(String name, String surname, Pageable pageable);

  /**
   * Updates user information.
   *
   * @param id the ID of the user to update
   * @param dto the updated user data
   * @return the updated user
   */
  UserShortDto updateUser(Long id, UserUpdateDto dto);

  /**
   * Changes the active/inactive status of a user.
   *
   * @param id the ID of the user
   * @param active the new active status
   */
  void changeUserActiveStatus(Long id, boolean active);
}