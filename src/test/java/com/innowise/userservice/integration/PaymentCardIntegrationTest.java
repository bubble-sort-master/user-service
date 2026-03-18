package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PaymentCardIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  private static final String USER_CREATE_JSON = """
            {
              "name": "CardTest",
              "surname": "User",
              "birthDate": "1995-05-15",
              "email": "card.integration@test.com"
            }
            """;

  private static final String CARD_CREATE_JSON = """
            {
              "number": "4111111111111111",
              "holder": "Card Integration Test",
              "expirationDate": "2035-12-31"
            }
            """;

  @Test
  void fullCardFlow_shouldWorkEndToEnd() throws Exception {
    // create user
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    // create cards
    String cardLocation = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long cardId = Long.parseLong(cardLocation.substring(cardLocation.lastIndexOf('/') + 1));

    // get by id
    mockMvc.perform(get("/api/cards/{cardId}", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numberMasked").exists());

    // get by user
    mockMvc.perform(get("/api/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numberMasked").exists());

    // update
    mockMvc.perform(put("/api/cards/{cardId}", cardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "holder": "Updated Holder Name",
                                  "expirationDate": "2035-12-31",
                                  "active": true
                                }
                                """))
            .andExpect(status().isOk());

    // change status
    mockMvc.perform(patch("/api/cards/{cardId}/active", cardId)
                    .param("active", "false"))
            .andExpect(status().isNoContent());
  }

  @Test
  void createCard_maxLimitExceeded_shouldReturnBadRequest() throws Exception {
    //create user
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "name": "CardTest",
                              "surname": "User",
                              "birthDate": "1995-05-15",
                              "email": "card.integration@test.com"
                            }
                            """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));


    int maxCards = 5;

    // creating max cards
    for (int i = 1; i <= maxCards; i++) {
      String cardNumber = String.format("411111111111%04d", i);
      mockMvc.perform(post("/api/users/{userId}/cards", userId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(String.format("""
                                {
                                  "number": "%s",
                                  "holder": "Card Integration Test %d",
                                  "expirationDate": "2035-12-31"
                                }
                                """, cardNumber, i)))
              .andExpect(status().isCreated());
    }

// exceeding limit
    String extraCardNumber = "4111111111119999";
    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                        {
                          "number": "%s",
                          "holder": "Extra Card",
                          "expirationDate": "2035-12-31"
                        }
                        """, extraCardNumber)))
            .andExpect(status().isConflict());
  }
}