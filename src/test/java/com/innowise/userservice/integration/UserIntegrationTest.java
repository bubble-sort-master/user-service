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

    mockMvc.perform(delete("/api/users/{id}", userId)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/{id}", userId)
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/users?page=0&size=20")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", not(hasItem(userId.intValue()))));
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

  @Test
  void getUserByEmail_shouldReturnUser() throws Exception {
    String adminToken = createAdminToken();

    String location = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

    mockMvc.perform(get("/api/users/by-email/user.integration@test.com")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("user.integration@test.com"))
            .andExpect(jsonPath("$.cards").value(nullValue()));

    String userToken = createUserToken(userId);
    mockMvc.perform(get("/api/users/by-email/user.integration@test.com")
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk());

    String secondUserEmail = "second.user@test.com";
    mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Second",
                                  "surname": "User",
                                  "birthDate": "1995-05-15",
                                  "email": "%s"
                                }
                                """.formatted(secondUserEmail)))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/users/by-email/" + secondUserEmail)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());
  }

  @Test
  void getUserByEmail_notFound_shouldReturn404() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(get("/api/users/by-email/nonexistent@test.com")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("User Not Found"));
  }

  @Test
  void getUsersByIds_shouldReturnListOfUsers() throws Exception {
    String adminToken = createAdminToken();

    String firstLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Bulk",
                                  "surname": "One",
                                  "birthDate": "1990-01-01",
                                  "email": "bulk.one@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    Long firstId = Long.parseLong(firstLocation.substring(firstLocation.lastIndexOf('/') + 1));

    String secondLocation = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Bulk",
                                  "surname": "Two",
                                  "birthDate": "1990-01-01",
                                  "email": "bulk.two@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    Long secondId = Long.parseLong(secondLocation.substring(secondLocation.lastIndexOf('/') + 1));

    mockMvc.perform(get("/api/users/bulk")
                    .param("ids", firstId.toString(), secondId.toString())
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].id", containsInAnyOrder(firstId.intValue(), secondId.intValue())))
            .andExpect(jsonPath("$[*].email", containsInAnyOrder("bulk.one@test.com", "bulk.two@test.com")));

    String userToken = createUserToken(firstId);
    mockMvc.perform(get("/api/users/bulk")
                    .param("ids", "1", "2")
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/users/bulk")
                    .param("ids", "1", "2"))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void getUsersByIds_emptyList_shouldReturnEmptyArray() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(get("/api/users/bulk")
                    .param("ids", "")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void deleteUser_shouldSoftDeleteAndHideFromQueries() throws Exception {
    String adminToken = createAdminToken();

    String location = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

    Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    String userToken = createUserToken(userId);

    mockMvc.perform(delete("/api/users/{id}", userId)
                    .header("Authorization", bearer(userToken)))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/{id}", userId)
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/users/by-email/user.integration@test.com")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/users")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", not(hasItem(userId.intValue()))));
  }

  @Test
  void deleteUser_notFound_shouldReturn404() throws Exception {
    String adminToken = createAdminToken();

    mockMvc.perform(delete("/api/users/99999")
                    .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("User Not Found"));
  }

  @Test
  void deleteUser_regularUserCannotDeleteOtherUser() throws Exception {
    String adminToken = createAdminToken();

    String loc1 = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_CREATE_JSON))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    Long user1Id = Long.parseLong(loc1.substring(loc1.lastIndexOf('/') + 1));
    String user1Token = createUserToken(user1Id);

    String loc2 = mockMvc.perform(post("/api/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Second",
                                  "surname": "User",
                                  "birthDate": "1995-05-15",
                                  "email": "second@test.com"
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    Long user2Id = Long.parseLong(loc2.substring(loc2.lastIndexOf('/') + 1));

    mockMvc.perform(delete("/api/users/{id}", user2Id)
                    .header("Authorization", bearer(user1Token)))
            .andExpect(status().isForbidden());
  }
}