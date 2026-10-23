package mate.academy.repository.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mate.academy.repository.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
        scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find user by existing email")
    void findByEmail_ExistingEmail_ReturnsUser() {
        // Given
        String email = "customer@example.com";

        // When
        var actual = userRepository.findByEmail(email);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(email, actual.get().getEmail());
    }

    @Test
    @DisplayName("Report existing email")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Given
        String email = "customer@example.com";

        // When
        boolean exists = userRepository.existsByEmail(email);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Report missing email")
    void existsByEmail_MissingEmail_ReturnsFalse() {
        // Given
        String email = "missing@example.com";

        // When
        boolean exists = userRepository.existsByEmail(email);

        // Then
        assertFalse(exists);
    }
}
