package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String USER_CREATE_JSON = """
            {
              "name": "Integration",
              "surname": "Test",
              "birthDate": "1995-05-15",
              "email": "user.integration@test.com"
            }
            """;

  @Test
  void fullUserFlow_shouldWorkEndToEnd() throws Exception {
    // create
    String location = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

    // get by id
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.birthDate").value("1995-05-15"))
            .andExpect(jsonPath("$.cards").value(nullValue()));

    // get all
    mockMvc.perform(get("/api/users?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].cards").value(nullValue()));

    // update
    mockMvc.perform(put("/api/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Updated Name",
                                  "surname": "Updated Surname",
                                  "birthDate": "1995-05-15",
                                  "email": "user.integration@test.com"
                                }
                                """))
            .andExpect(status().isOk());

    // change status
    mockMvc.perform(patch("/api/users/{id}/active", userId)
                    .param("active", "false"))
            .andExpect(status().isNoContent());
  }
}