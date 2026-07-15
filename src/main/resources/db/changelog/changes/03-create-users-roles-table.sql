CREATE TABLE users_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_users_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);
