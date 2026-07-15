package mate.academy.repository;

import mate.academy.config.CustomMySqlContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractRepositoryTest {
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        CustomMySqlContainer container = CustomMySqlContainer.getInstance();
        container.start();

        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
    }
}
