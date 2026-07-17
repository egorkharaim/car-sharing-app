package mate.academy.controller.payment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mate.academy.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class PaymentControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get payments as customer returns own payments only")
    void getPayments_CustomerUser_ReturnsOwnPayments() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/payments")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rentalId", is(1)))
                .andExpect(jsonPath("$.content[1].rentalId", is(2)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get payments as customer ignores user id filter and returns own payments")
    void getPayments_CustomerUserWithUserId_ReturnsOwnPayments() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/payments")
                        .param("userId", "3")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rentalId", is(1)))
                .andExpect(jsonPath("$.content[1].rentalId", is(2)));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Get payments as manager with user id filter returns selected user payments")
    void getPayments_ManagerWithUserId_ReturnsFilteredPayments() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/payments")
                        .param("userId", "3")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rentalId", is(3)));
    }

    @Test
    @DisplayName("Successful payment redirect without existing session returns not found")
    void handleSuccessfulPayment_MissingSession_ReturnsNotFound() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/payments/success")
                        .param("session_id", "missing-session"))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is(
                        "Can't find payment by session id: missing-session"
                )));
    }

    @Test
    @DisplayName("Canceled payment redirect returns payment by session id")
    void handleCanceledPayment_ExistingSession_ReturnsPayment() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/payments/cancel")
                        .param("session_id", "session-1"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.type", is("PAYMENT")))
                .andExpect(jsonPath("$.rentalId", is(1)));
    }
}
