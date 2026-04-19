package com.innowise.userservice.security;

import com.innowise.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthorization {

  private final UserRepository userRepository;

  public boolean isOwnerByEmail(String email, Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return false;
    }
    return userRepository.findByEmail(email)
            .map(user -> user.getId().toString().equals(authentication.getName()))
            .orElse(false);
  }
}