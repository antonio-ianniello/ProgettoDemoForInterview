-- Drop old structure
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

-- Enum dei ruoli applicativi
CREATE TYPE roles_type AS ENUM (
    'OWNER',
    'OPERATOR',
    'MAINTAINER',
    'DEVELOPER',
    'REPORTER'
);

-- Tabella utenti
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    surname     VARCHAR(100) NOT NULL,
    tax_code    VARCHAR(16)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabella ruoli (pre-popolata con i valori dell'enum)
CREATE TABLE roles (
    id   BIGSERIAL   PRIMARY KEY,
    name roles_type  NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES
    ('OWNER'),
    ('OPERATOR'),
    ('MAINTAINER'),
    ('DEVELOPER'),
    ('REPORTER');

-- Tabella di associazione many-to-many
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);
