package com.innowise.userservice.integration;

import com.innowise.userservice.security.JwtAuthenticationConverter;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.impl.PaymentCardServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

  @Value("${jwt.secret}")
  private String secret;

  @Bean
  @Primary
  public JwtEncoder jwtEncoder() {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtEncoder.withSecretKey(key).build();
  }

  @Bean(name = "jwtDecoder")
  @Primary
  public JwtDecoder jwtDecoder() {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  @Bean
  @Primary
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    return new JwtAuthenticationConverter();
  }

  @Bean(name = "paymentCardService")
  @Primary
  public PaymentCardService paymentCardService(PaymentCardServiceImpl impl) {
    return impl;
  }
}