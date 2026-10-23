--liquibase formatted sql
--changeset egorkharaim:insert-default-roles
INSERT INTO roles (name)
VALUES ('MANAGER'),
       ('CUSTOMER');
