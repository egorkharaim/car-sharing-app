package mate.academy.repository.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mate.academy.model.user.RoleName;
import mate.academy.repository.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Find role by name")
    void findByName_ExistingRoleName_ReturnsRole() {
        // Given
        RoleName roleName = RoleName.CUSTOMER;

        // When
        var actual = roleRepository.findByName(roleName);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(roleName, actual.get().getName());
    }
}
