package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.UserCreateDto;
import com.innowise.userservice.dto.UserShortDto;
import com.innowise.userservice.dto.UserUpdateDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateUserException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import com.innowise.userservice.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private static final String USERS_CACHE                  = "users";
  private static final String USER_BY_ID_KEY               = "'byId::' + #id";

  private static final String ALL_USERS_SHORT_KEY =
          "'all::short::name:' + (#name ?: '') + '::surname:' + (#surname ?: '') + '::page:' + #pageable.pageNumber + '::size:' + #pageable.pageSize";

  private static final String ALL_USERS_WITH_CARDS_KEY =
          "'all::withcards::name:' + (#name ?: '') + '::surname:' + (#surname ?: '') + '::page:' + #pageable.pageNumber + '::size:' + #pageable.pageSize";

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  @CacheEvict(value = USERS_CACHE, allEntries = true)
  public UserShortDto createUser(UserCreateDto dto) {
    if (userRepository.existsByEmail(dto.email())) {
      throw new DuplicateUserException("User with email " + dto.email() + " already exists");
    }

    User user = userMapper.toEntity(dto);
    user.setActive(true);
    User saved = userRepository.save(user);
    return userMapper.toShortDto(saved);
  }

  @Override
  @Cacheable(value = USERS_CACHE, key = USER_BY_ID_KEY)
  public UserWithCardsDto getUserById(Long id) {
    User user = userRepository.findByIdWithCards(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    return userMapper.toWithCardsDto(user);
  }

  @Override
  @Cacheable(value = USERS_CACHE, key = ALL_USERS_SHORT_KEY)
  public Page<UserShortDto> getAllUsers(String name, String surname, Pageable pageable) {
    Specification<User> spec = UserSpecifications.searchByNameAndSurname(name, surname);
    return userRepository.findAll(spec, pageable)
            .map(userMapper::toShortDto);
  }

  @Override
  @Cacheable(value = USERS_CACHE, key = ALL_USERS_WITH_CARDS_KEY)
  public Page<UserWithCardsDto> getAllUsersWithCards(String name, String surname, Pageable pageable) {
    Specification<User> spec = UserSpecifications.searchByNameAndSurname(name, surname);

    return userRepository.findAllWithCards(spec, pageable)
            .map(userMapper::toWithCardsDto);
  }

  @Override
  @Transactional
  @CacheEvict(value = USERS_CACHE, allEntries = true)
  public UserShortDto updateUser(Long id, UserUpdateDto dto) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

    userMapper.updateFromDto(dto, user);
    User updated = userRepository.save(user);
    return userMapper.toShortDto(updated);
  }

  @Override
  @Transactional
  @CacheEvict(value = USERS_CACHE, allEntries = true)
  public void changeUserActiveStatus(Long id, boolean active) {
    if (!userRepository.existsById(id)) {
      throw new UserNotFoundException(id);
    }
    userRepository.setActive(id, active);
  }
}