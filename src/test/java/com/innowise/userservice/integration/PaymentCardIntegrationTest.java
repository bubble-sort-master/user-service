package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PaymentCardIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

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
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert userLocation != null;
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    String cardLocation = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert cardLocation != null;
    Long cardId = Long.parseLong(cardLocation.substring(cardLocation.lastIndexOf('/') + 1));

    mockMvc.perform(get("/api/cards/{cardId}", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numberMasked").exists());

    mockMvc.perform(get("/api/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numberMasked").exists());

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

    mockMvc.perform(patch("/api/cards/{cardId}", cardId)
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
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert userLocation != null;
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/cards")
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].numberMasked").exists());
  }

  @Test
  void getAllCards_withFilters_shouldReturnFiltered() throws Exception {
    String aliceLocation = mockMvc.perform(post("/api/users")
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
    assert aliceLocation != null;
    Long aliceId = Long.parseLong(aliceLocation.substring(aliceLocation.lastIndexOf('/') + 1));

    mockMvc.perform(post("/api/users/{userId}/cards", aliceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated());

    String bobLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Bob",
                                  "surname": "Jones",
                                  "birthDate": "1992-02-02",
                                  "email": "bob@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    assert bobLocation != null;
    Long bobId = Long.parseLong(bobLocation.substring(bobLocation.lastIndexOf('/') + 1));

    mockMvc.perform(post("/api/users/{userId}/cards", bobId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "number": "4222222222222222",
                                  "holder": "Bob Jones",
                                  "expirationDate": "2035-12-31"
                                }
                                """))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/cards")
                    .param("name", "Alice")
                    .param("surname", "Smith")
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].holder").value(containsString("Card")))
            .andExpect(jsonPath("$.content[0].holder").value(containsString("Test")));
  }

  @Test
  void createCard_maxLimitExceeded_shouldReturnBadRequest() throws Exception {
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "CardTest",
                                  "surname": "User",
                                  "birthDate": "1995-05-15",
                                  "email": "card.maxlimit@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert userLocation != null;
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    int maxCards = 5;

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

  @Test
  void getCardById_notFound_shouldReturn404() throws Exception {
    mockMvc.perform(get("/api/cards/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Card Not Found"))
            .andExpect(jsonPath("$.detail").value(containsString("Card not found with id: 99999")));
  }

  @Test
  void changeCardActiveStatus_invalidBody_shouldReturn400() throws Exception {
    String userLocation = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert userLocation != null;
    Long userId = Long.parseLong(userLocation.substring(userLocation.lastIndexOf('/') + 1));

    String cardLocation = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CARD_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert cardLocation != null;
    Long cardId = Long.parseLong(cardLocation.substring(cardLocation.lastIndexOf('/') + 1));

    mockMvc.perform(patch("/api/cards/{cardId}", cardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value(containsString("active")))
            .andExpect(jsonPath("$.errors[0]").value(containsString("required")));
  }
}