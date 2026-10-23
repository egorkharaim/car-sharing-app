package mate.academy.controller.car;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.dto.car.CreateCarRequestDto;
import mate.academy.model.CarType;
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
class CarControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Get all cars returns paged cars for public user")
    void getAll_PublicUser_ReturnsCarsPage() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/cars")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].model", is("Camry")))
                .andExpect(jsonPath("$.content[0].brand", is("Toyota")));
    }

    @Test
    @DisplayName("Get car by id returns car for public user")
    void getById_ExistingCar_ReturnsCar() throws Exception {
        // Given

        // When
        mockMvc.perform(get("/cars/{id}", 1))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.model", is("Camry")))
                .andExpect(jsonPath("$.brand", is("Toyota")))
                .andExpect(jsonPath("$.type", is("SEDAN")));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Create car as manager returns created car")
    void create_ManagerUser_ReturnsCreatedCar() throws Exception {
        // Given
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "Civic",
                "Honda",
                CarType.SEDAN,
                4,
                BigDecimal.valueOf(55)
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/cars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model", is("Civic")))
                .andExpect(jsonPath("$.brand", is("Honda")))
                .andExpect(jsonPath("$.type", is("SEDAN")))
                .andExpect(jsonPath("$.inventory", is(4)))
                .andExpect(jsonPath("$.dailyFee", is(55)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Create car as customer returns forbidden")
    void create_CustomerUser_ReturnsForbidden() throws Exception {
        // Given
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "Civic",
                "Honda",
                CarType.SEDAN,
                4,
                BigDecimal.valueOf(55)
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/cars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Then
                .andExpect(status().isForbidden());
    }
}
