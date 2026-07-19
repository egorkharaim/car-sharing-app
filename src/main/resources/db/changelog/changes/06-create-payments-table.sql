--liquibase formatted sql
--changeset egorkharaim:create-payments-table
CREATE TABLE payments
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    rental_id BIGINT NOT NULL,
    session_url VARCHAR(500),
    session_id VARCHAR(255) UNIQUE,
    amount_to_pay DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (id)
);
