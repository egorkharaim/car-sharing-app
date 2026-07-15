CREATE TABLE rentals
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    rental_date DATE NOT NULL,
    return_date DATE NOT NULL,
    actual_return_date DATE,
    car_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rentals_car
        FOREIGN KEY (car_id) REFERENCES cars (id),
    CONSTRAINT fk_rentals_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
