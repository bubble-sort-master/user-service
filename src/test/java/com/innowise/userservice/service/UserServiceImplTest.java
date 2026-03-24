package com.innowise.userservice.service;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateUserException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.impl.UserServiceImpl;
import com.innowise.userservice.specification.UserSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;

  @InjectMocks private UserServiceImpl userService;

  private UserCreateDto createDto;
  private UserUpdateDto updateDto;
  private User userEntity;
  private UserShortDto shortDto;
  private UserWithCardsDto withCardsDto;

  @BeforeEach
  void setUp() {
    createDto = new UserCreateDto("John", "Doe", LocalDate.of(1990, 1, 1), "john.doe@example.com");
    updateDto = new UserUpdateDto("John Updated", "Doe", LocalDate.of(1990, 1, 1), "john.doe@example.com");

    userEntity = new User();
    userEntity.setId(1L);
    userEntity.setName("John");
    userEntity.setSurname("Doe");
    userEntity.setBirthDate(LocalDate.of(1990, 1, 1));
    userEntity.setEmail("john.doe@example.com");
    userEntity.setActive(true);

    shortDto = new UserShortDto(1L, "John", "Doe", "john.doe@example.com", true, 0);
    withCardsDto = new UserWithCardsDto(
            1L, "John", "Doe", LocalDate.of(1990, 1, 1),
            "john.doe@example.com", true, List.of()
    );
  }

  @Test
  void createUser_success() {
    when(userRepository.existsByEmail(createDto.email())).thenReturn(false);
    when(userMapper.toEntity(createDto)).thenReturn(userEntity);
    when(userRepository.save(any(User.class))).thenReturn(userEntity);
    when(userMapper.toShortDto(userEntity)).thenReturn(shortDto);

    UserShortDto result = userService.createUser(createDto);

    assertThat(result).isEqualTo(shortDto);
    verify(userRepository).existsByEmail(createDto.email());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void createUser_duplicateEmail_throwsDuplicateUserException() {
    when(userRepository.existsByEmail(createDto.email())).thenReturn(true);

    assertThatThrownBy(() -> userService.createUser(createDto))
            .isInstanceOf(DuplicateUserException.class)
            .hasMessageContaining("User with email " + createDto.email() + " already exists");
  }

  @Test
  void getUserById_success() {
    when(userRepository.findWithPaymentCardsById(1L)).thenReturn(Optional.of(userEntity));
    when(userMapper.toWithCardsDto(userEntity)).thenReturn(withCardsDto);

    UserWithCardsDto result = userService.getUserById(1L);

    assertThat(result).isEqualTo(withCardsDto);
  }

  @Test
  void getUserById_notFound_throwsUserNotFoundException() {
    when(userRepository.findWithPaymentCardsById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void getAllUsers_success() {
    Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
    Page<User> page = new PageImpl<>(List.of(userEntity));

    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
    when(userMapper.toWithCardsDto(any(User.class))).thenReturn(withCardsDto);

    Page<UserWithCardsDto> result = userService.getAllUsers("John", "Doe", pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst()).isEqualTo(withCardsDto);
  }

  @Test
  void updateUser_success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
    when(userRepository.save(any(User.class))).thenReturn(userEntity);
    when(userMapper.toShortDto(userEntity)).thenReturn(shortDto);

    UserShortDto result = userService.updateUser(1L, updateDto);

    assertThat(result).isEqualTo(shortDto);
    verify(userMapper).updateFromDto(updateDto, userEntity);
  }

  @Test
  void updateUser_notFound_throwsUserNotFoundException() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateUser(999L, updateDto))
            .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void changeUserActiveStatus_success() {
    when(userRepository.existsById(1L)).thenReturn(true);

    userService.changeUserActiveStatus(1L, false);

    verify(userRepository).setActive(1L, false);
  }

  @Test
  void changeUserActiveStatus_notFound_throwsUserNotFoundException() {
    when(userRepository.existsById(999L)).thenReturn(false);

    assertThatThrownBy(() -> userService.changeUserActiveStatus(999L, true))
            .isInstanceOf(UserNotFoundException.class);
  }
}
