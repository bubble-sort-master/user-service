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

  @Test
  void createUser_duplicateEmail_shouldReturnConflict() throws Exception {
    // first creation
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "First",
                                  "surname": "User",
                                  "birthDate": "1990-01-01",
                                  "email": "duplicate@test.com"
                                }
                                """))
            .andExpect(status().isCreated());

    // duplicate attempt
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Second",
                                  "surname": "User",
                                  "birthDate": "1990-01-01",
                                  "email": "duplicate@test.com"
                                }
                                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("User Already Exists"))
            .andExpect(jsonPath("$.detail").value(containsString("already exists")));
  }

  @Test
  void getUserById_notFound_shouldReturn404() throws Exception {
    mockMvc.perform(get("/api/users/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("User Not Found"))
            .andExpect(jsonPath("$.detail").value(containsString("User not found with id: 99999")));
  }

  @Test
  void createUser_invalidData_shouldReturn400() throws Exception {
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                      "name": "",
                      "surname": "",
                      "birthDate": "2000-01-01",
                      "email": "not-an-email"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
  }
}