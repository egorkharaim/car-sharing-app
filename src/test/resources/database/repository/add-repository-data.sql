INSERT INTO users (id, email, first_name, last_name, password)
VALUES (1, 'customer@example.com', 'Customer', 'One', 'password'),
       (2, 'another.customer@example.com', 'Customer', 'Two', 'password');

INSERT INTO users_roles (user_id, role_id)
VALUES (1, 2),
       (2, 2);

INSERT INTO cars (id, model, brand, type, inventory, daily_fee)
VALUES (1, 'Camry', 'Toyota', 'SEDAN', 2, 40.00),
       (2, 'RAV4', 'Toyota', 'SUV', 0, 70.00);

INSERT INTO rentals (id, rental_date, return_date, actual_return_date, car_id, user_id)
VALUES (1, '2026-07-01', '2026-07-05', NULL, 1, 1),
       (2, '2026-06-01', '2026-06-05', '2026-06-06', 1, 1),
       (3, '2026-07-10', '2026-07-15', NULL, 2, 2);

INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay)
VALUES (1, 'PENDING', 'PAYMENT', 1, 'https://checkout.stripe.com/session-1', 'session-1', 160.00),
       (2, 'PAID', 'FINE', 2, 'https://checkout.stripe.com/session-2', 'session-2', 40.00),
       (3, 'PENDING', 'PAYMENT', 3, NULL, NULL, 350.00);
