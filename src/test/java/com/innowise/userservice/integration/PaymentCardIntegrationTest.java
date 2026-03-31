package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PaymentCardIntegrationTest extends AbstractIntegrationTest {

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
    String adminToken = createAdminToken();

    String userLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert userLocation != null;
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    String userToken = createUserToken(userId);

    String cardLocation = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert cardLocation != null;
    Long cardId = Long.parseLong(cardLocation.substring(cardLocation.lastIndexOf('/') + 1));

    mockMvc.perform(get("/api/cards/{cardId}", cardId)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numberMasked").exists());

    mockMvc.perform(get("/api/users/{userId}/cards", userId)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numberMasked").exists());

    mockMvc.perform(put("/api/cards/{cardId}", cardId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "holder": "Updated Holder Name",
                                  "expirationDate": "2035-12-31",
                                  "active": true
                                }
                                """))
            .andExpect(status().isOk());

    mockMvc.perform(patch("/api/cards/{cardId}", cardId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "active": false
                                }
                                """))
            .andExpect(status().isNoContent());
  }

  @Test
  void getAllCards_shouldReturnPage() throws Exception {
    String adminToken = createAdminToken();

    String userLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .header("Authorization", bearer(createUserToken(userId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/cards")
                    .header("Authorization", bearer(adminToken))
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getAllCards_withFilters_shouldReturnFiltered() throws Exception {
    String adminToken = createAdminToken();

    String aliceLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Alice",
                                  "surname": "Smith",
                                  "birthDate": "1990-01-01",
                                  "email": "alice@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long aliceId = Long.parseLong(aliceLocation.substring(aliceLocation.lastIndexOf('/') + 1));

    mockMvc.perform(post("/api/users/{userId}/cards", aliceId)
                    .header("Authorization", bearer(createUserToken(aliceId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/cards")
                    .header("Authorization", bearer(adminToken))
                    .param("name", "Alice")
                    .param("surname", "Smith")
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void createCard_maxLimitExceeded_shouldReturnBadRequest() throws Exception {
    String adminToken = createAdminToken();
    String userLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));
    String userToken = createUserToken(userId);

    int maxCards = 5;
    for (int i = 1; i <= maxCards; i++) {
      String cardNumber = String.format("411111111111%04d", i);
      mockMvc.perform(post("/api/users/{userId}/cards", userId)
                      .header("Authorization", bearer(userToken))
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

    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "number": "4111111111119999",
                                  "holder": "Extra Card",
                                  "expirationDate": "2035-12-31"
                                }
                                """))
            .andExpect(status().isConflict());
  }

  @Test
  void getCardById_notFound_shouldReturn404() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(get("/api/cards/99999")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());
  }

  @Test
  void changeCardActiveStatus_invalidBody_shouldReturn400() throws Exception {
    String adminToken = createAdminToken();
    String userLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));
    String userToken = createUserToken(userId);

    String cardLocation = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long cardId = Long.parseLong(cardLocation.substring(cardLocation.lastIndexOf('/') + 1));

    mockMvc.perform(patch("/api/cards/{cardId}", cardId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest());
  }
}