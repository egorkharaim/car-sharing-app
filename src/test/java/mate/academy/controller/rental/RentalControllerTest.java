package mate.academy.controller.rental;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.dto.rental.CreateRentalRequestDto;
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
class RentalControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get current customer rentals returns own rentals")
    void getRentals_CustomerUser_ReturnsOwnRentals() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/rentals")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].userId", is(1)))
                .andExpect(jsonPath("$.content[1].userId", is(1)));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Get rentals as manager with user id filter returns selected user rentals")
    void getRentals_ManagerUserWithUserId_ReturnsFilteredRentals() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/rentals")
                        .param("userId", "3")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(3)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get another customer rental by id returns not found")
    void getRentalById_AnotherCustomerRental_ReturnsNotFound() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/rentals/{id}", 3))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("You don't have access to this rental")));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Create rental as manager user with no active rental returns created rental")
    void createRental_ManagerWithoutActiveRental_ReturnsCreatedRental() throws Exception {
        // Given
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                3L,
                LocalDate.now().plusDays(3)
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/rentals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carId", is(3)))
                .andExpect(jsonPath("$.userId", is(2)))
                .andExpect(jsonPath("$.actualReturnDate").isEmpty());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Create rental with active rental returns bad request")
    void createRental_CustomerWithActiveRental_ReturnsBadRequest() throws Exception {
        // Given
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                3L,
                LocalDate.now().plusDays(3)
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/rentals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(
                        "User already has an active rental and can't create a new one"
                )));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Return overdue rental returns rental with actual return date")
    void returnRental_OverdueRental_ReturnsReturnedRental() throws Exception {
        // Given

        // When
        mockMvc.perform(patch("/rentals/{id}/return", 1)
                        .with(csrf()))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.actualReturnDate").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Return already returned rental returns bad request")
    void returnRental_AlreadyReturnedRental_ReturnsBadRequest() throws Exception {
        // Given

        // When
        mockMvc.perform(patch("/rentals/{id}/return", 2)
                        .with(csrf()))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Rental has already been returned")));
    }
}
