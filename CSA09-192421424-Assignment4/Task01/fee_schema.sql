-- Task 1: schema for JdbcPaymentRepository (optional part of the demo)
CREATE DATABASE IF NOT EXISTS fees;
USE fees;

CREATE TABLE IF NOT EXISTS fee_due (
    student_id  VARCHAR(10)   NOT NULL PRIMARY KEY,
    outstanding DECIMAL(10,2) NOT NULL CHECK (outstanding >= 0)
);

CREATE TABLE IF NOT EXISTS fee_payment (
    receipt_no INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(10)   NOT NULL,
    amount     DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    method     VARCHAR(10)   NOT NULL,
    paid_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES fee_due(student_id)
);

DELETE FROM fee_payment;
DELETE FROM fee_due;
INSERT INTO fee_due (student_id, outstanding) VALUES
    ('192421424', 50000.00), ('192421425', 12500.50), ('192421426', 0.00);
