package mate.academy.controller.user;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.dto.user.UserRegistrationRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
class AuthenticationControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Register new customer returns created user dto")
    void register_NewCustomer_ReturnsUserResponseDto() throws Exception {
        // Given
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "new.customer@example.com",
                "New",
                "Customer",
                "password123",
                "password123"
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("new.customer@example.com")))
                .andExpect(jsonPath("$.firstName", is("New")))
                .andExpect(jsonPath("$.lastName", is("Customer")))
                .andExpect(jsonPath("$.roles", hasItem("CUSTOMER")));
    }

    @Test
    @DisplayName("Register existing email returns conflict")
    void register_ExistingEmail_ReturnsConflict() throws Exception {
        // Given
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "customer@example.com",
                "Customer",
                "One",
                "password123",
                "password123"
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is(
                        "This email is already taken: customer@example.com"
                )));
    }
}
