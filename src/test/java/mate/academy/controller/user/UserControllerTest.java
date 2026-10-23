package mate.academy.controller.user;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.dto.user.UpdateUserProfileRequestDto;
import mate.academy.dto.user.UpdateUserRoleRequestDto;
import mate.academy.model.user.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(
        scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get current user profile returns authenticated user")
    void getProfile_AuthenticatedCustomer_ReturnsProfile() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/users/me"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("customer@example.com")))
                .andExpect(jsonPath("$.firstName", is("Customer")))
                .andExpect(jsonPath("$.lastName", is("One")))
                .andExpect(jsonPath("$.roles", hasItem("CUSTOMER")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Update current user profile returns updated profile")
    void updateProfile_AuthenticatedCustomer_ReturnsUpdatedProfile() throws Exception {
        // Given
        UpdateUserProfileRequestDto requestDto = new UpdateUserProfileRequestDto(
                "Updated",
                "Customer"
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("customer@example.com")))
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Customer")));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Update user role as manager returns updated user")
    void updateRole_ManagerUser_ReturnsUpdatedUser() throws Exception {
        // Given
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto(RoleName.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(patch("/users/{id}/role", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("customer@example.com")))
                .andExpect(jsonPath("$.roles", hasItem("MANAGER")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Update user role as customer returns forbidden")
    void updateRole_CustomerUser_ReturnsForbidden() throws Exception {
        // Given
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto(RoleName.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(patch("/users/{id}/role", 3)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isForbidden());
    }
}
