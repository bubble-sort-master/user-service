package com.innowise.userservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public record JwtUserPrincipal(
        Long userId,
        String role
) {

  public static JwtUserPrincipal fromJwt(Jwt jwt) {
    Long userId = Long.valueOf(jwt.getSubject());
    String role = jwt.getClaimAsString("role");
    return new JwtUserPrincipal(userId, role);
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }
}