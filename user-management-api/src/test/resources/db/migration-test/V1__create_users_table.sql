CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    tax_code VARCHAR(32) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_user_roles_role ON user_roles (role);
