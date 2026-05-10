package com.innowise.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestPropertySource(properties = {"internal.secret=test-internal-secret"})
class InternalRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_SECRET = "test-internal-secret";
    private static final String INVALID_SECRET = "wrong-secret";

    private static final String USER_CREATE_JSON = """
            {
              "name": "Rollback",
              "surname": "Test",
              "birthDate": "1990-01-01",
              "email": "rollback.user@test.com"
            }
            """;

    @Test
    void rollbackUser_WithValidSecret_ShouldDeactivateAndReturnNoContent() throws Exception {
        String adminToken = createAdminToken();

        String location = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        assert location != null;
        Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/internal/users/{id}/rollback", userId)
                        .header("X-Internal-Secret", VALID_SECRET))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rollbackUser_WithInvalidSecret_ShouldReturnForbidden() throws Exception {
        String adminToken = createAdminToken();

        String location = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(delete("/api/internal/users/{id}/rollback", userId)
                        .header("X-Internal-Secret", INVALID_SECRET))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void rollbackUser_MissingSecretHeader_ShouldReturnForbidden() throws Exception {
        String adminToken = createAdminToken();

        String location = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(delete("/api/internal/users/{id}/rollback", userId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void rollbackUser_NonExistingUser_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/internal/users/99999/rollback")
                        .header("X-Internal-Secret", VALID_SECRET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }
}