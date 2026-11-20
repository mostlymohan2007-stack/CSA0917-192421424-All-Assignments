-- ============================================================
-- Assignment 5 - LOCAL TEST DATABASE (used only for this assignment)
-- Run:  mysql -u root -p < test_database.sql
-- Never run this against a real database: it drops and recreates the tables.
-- ============================================================
CREATE DATABASE IF NOT EXISTS transport_test CHARACTER SET utf8mb4;
USE transport_test;

-- B2: login table (test accounts only; real systems must use salted bcrypt/argon2, not plain SHA-256)
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
    username      VARCHAR(30) NOT NULL,
    password_hash CHAR(64)    NOT NULL,
    role          VARCHAR(20) NOT NULL,
    PRIMARY KEY (username)
);
INSERT INTO app_user (username, password_hash, role) VALUES
    ('admin', SHA2('Admin@123', 256), 'ADMIN'),
    ('clerk', SHA2('Clerk@456', 256), 'CLERK');

-- B3 / B4: bus pass table
DROP TABLE IF EXISTS bus_pass;
CREATE TABLE bus_pass (
    pass_id    VARCHAR(10)   NOT NULL,
    reg_no     VARCHAR(12)   NOT NULL,
    name       VARCHAR(60)   NOT NULL,
    route      VARCHAR(4)    NOT NULL,
    stop       VARCHAR(40)   NOT NULL,
    validity   DATE          NOT NULL,
    amount     DECIMAL(8,2)  NOT NULL,
    PRIMARY KEY (pass_id),
    KEY idx_route (route),
    CONSTRAINT chk_amount CHECK (amount > 0)
);
