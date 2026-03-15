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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final CacheManager cacheManager;

  @Override
  @Transactional
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
  @Cacheable(value = "users", key = "'byId::' + #id")
  public UserWithCardsDto getUserById(Long id) {
    User user = userRepository.findByIdWithCards(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    return userMapper.toWithCardsDto(user);
  }

  @Override
  public Page<UserShortDto> getAllUsers(String name, String surname, Pageable pageable) {
    Specification<User> spec = UserSpecifications.searchByNameAndSurname(name, surname);
    return userRepository.findAll(spec, pageable)
            .map(userMapper::toShortDto);
  }

  @Override
  @Transactional
  @CacheEvict(value = "users", key = "'byId::' + #id")
  public UserShortDto updateUser(Long id, UserUpdateDto dto) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

    userMapper.updateFromDto(dto, user);
    User updated = userRepository.save(user);
    return userMapper.toShortDto(updated);
  }

  @Override
  @Transactional
  public void changeUserActiveStatus(Long id, boolean active) {
    if (!userRepository.existsById(id)) {
      throw new UserNotFoundException(id);
    }
    userRepository.setActive(id, active);

    evictUserCache(id);
  }

  private void evictUserCache(Long id) {
    Cache cache = cacheManager.getCache("users");
    if (cache != null) {
      cache.evict("byId::" + id);
    }
  }
}