package mate.academy.config;

import org.testcontainers.containers.MySQLContainer;

public class CustomMySqlContainer extends MySQLContainer<CustomMySqlContainer> {
    private static final String MYSQL_IMAGE = "mysql:8.0.33";
    private static final String DATABASE_NAME = "car_sharing_test";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static final CustomMySqlContainer INSTANCE = new CustomMySqlContainer();

    private CustomMySqlContainer() {
        super(MYSQL_IMAGE);
        withDatabaseName(DATABASE_NAME);
        withUsername(USERNAME);
        withPassword(PASSWORD);
    }

    public static CustomMySqlContainer getInstance() {
        return INSTANCE;
    }
}
