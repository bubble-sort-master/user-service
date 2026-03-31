package com.innowise.userservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

  @NotBlank(message = "JWT secret is required")
  private String secret;

  @NotNull
  @Min(1000)
  private long accessTokenExpiration = 900000;

  @NotNull
  @Min(1000)
  private long refreshTokenExpiration = 604800000;
}
