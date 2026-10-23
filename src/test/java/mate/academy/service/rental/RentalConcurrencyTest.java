package mate.academy.service.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.dto.rental.CreateRentalRequestDto;
import mate.academy.exception.RentalProcessingException;
import mate.academy.model.user.User;
import mate.academy.repository.rental.RentalRepository;
import mate.academy.service.notification.NotificationMessageBuilder;
import mate.academy.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RentalConcurrencyTest extends AbstractIntegrationTest {
    @Autowired
    private RentalService rentalService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private NotificationMessageBuilder messageBuilder;

    @Test
    @DisplayName("Concurrent creates allow only one active rental per user")
    void createRental_ConcurrentRequests_CreatesOneRental() throws Exception {
        // Given
        User user = userService.getByEmail("manager@example.com");
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                1L, LocalDate.now().plusDays(3));

        // When
        int successful = runTogether(() -> rentalService.createRental(request, user));

        // Then
        assertEquals(1, successful);
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from rentals where user_id = 2", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select inventory from cars where id = 1", Integer.class));
    }

    @Test
    @DisplayName("Concurrent returns of one rental restore inventory only once")
    void returnRental_ConcurrentRequests_ReturnsOnlyOnce() throws Exception {
        // Given
        User user = userService.getByEmail("customer@example.com");

        // When
        int successful = runTogether(() -> rentalService.returnRental(1L, user));

        // Then
        assertEquals(1, successful);
        assertEquals(3, jdbcTemplate.queryForObject(
                "select inventory from cars where id = 1", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from payments where rental_id = 1 and type = 'FINE'",
                Integer.class));
    }

    @Test
    @DisplayName("Concurrent returns of different rentals preserve both inventory increments")
    void returnRental_DifferentRentals_RestoresBothCars() throws Exception {
        // Given
        User manager = userService.getByEmail("manager@example.com");
        jdbcTemplate.update("update rentals set car_id = 1 where id = 3");

        // When
        int successful = runTogether(
                () -> rentalService.returnRental(1L, manager),
                () -> rentalService.returnRental(3L, manager));

        // Then
        assertEquals(2, successful);
        assertEquals(4, jdbcTemplate.queryForObject(
                "select inventory from cars where id = 1", Integer.class));
    }

    @Test
    @DisplayName("Overdue notification data remains usable outside repository transaction")
    void overdueRentals_DetachedEntities_BuildMessages() {
        // Given
        // When
        var rentals = rentalRepository
                .findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate.now());

        // Then
        assertEquals(2, rentals.size());
        rentals.forEach(rental -> assertTrue(
                messageBuilder.buildOverdueRentalMessage(rental).contains("Toyota")));
    }

    private int runTogether(Callable<?> action) throws Exception {
        return runTogether(action, action);
    }

    private int runTogether(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.function.Function<Callable<?>, Callable<Integer>> task = action -> () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start");
            }
            try {
                action.call();
                return 1;
            } catch (RentalProcessingException e) {
                return 0;
            }
        };
        try {
            var first = executor.submit(task.apply(firstAction));
            var second = executor.submit(task.apply(secondAction));
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers are not ready");
            }
            start.countDown();
            return first.get(30, TimeUnit.SECONDS) + second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
