package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserIntegrationTest extends AbstractIntegrationTest {

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
    String adminToken = createAdminToken();

    String location = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    assert location != null;
    Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

    String userToken = createUserToken(userId);

    mockMvc.perform(get("/api/users/{id}", userId)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.birthDate").value("1995-05-15"))
            .andExpect(jsonPath("$.cards").value(nullValue()));

    mockMvc.perform(get("/api/users?page=0&size=20")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].cards").value(nullValue()));

    mockMvc.perform(put("/api/users/{id}", userId)
                    .header("Authorization", bearer(userToken))
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

    mockMvc.perform(patch("/api/users/{id}", userId)
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
  void createUser_duplicateEmail_shouldReturnConflict() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
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

    mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
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
            .andExpect(jsonPath("$.title").value("User Already Exists"));
  }

  @Test
  void getUserById_notFound_shouldReturn404() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(get("/api/users/99999")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("User Not Found"));
  }

  @Test
  void createUser_invalidData_shouldReturn400() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
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
            .andExpect(jsonPath("$.title").value("Validation Failed"));
  }

  @Test
  void changeUserActiveStatus_invalidBody_shouldReturn400() throws Exception {
    String adminToken = createAdminToken();
    String location = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    String userToken = createUserToken(userId);

    mockMvc.perform(patch("/api/users/{id}", userId)
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"));
  }

  @Test
  void unauthenticatedUser_shouldGet401() throws Exception {
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void regularUser_cannotCreateUser() throws Exception {
    String userToken = createUserToken(100L);

    mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isForbidden());
  }
}