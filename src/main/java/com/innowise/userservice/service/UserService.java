package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  UserShortDto createUser(UserCreateDto dto);

  UserWithCardsDto getUserById(Long id);

  Page<UserShortDto> getAllUsers(String name, String surname, Pageable pageable);

  UserShortDto updateUser(Long id, UserUpdateDto dto);

  void changeUserActiveStatus(Long id, boolean active);
}