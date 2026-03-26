package com.innowise.userservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    JwtUserPrincipal principal = JwtUserPrincipal.fromJwt(jwt);

    return new JwtAuthenticationToken(
            jwt,
            principal.getAuthorities(),
            principal.userId().toString()
    );
  }
}