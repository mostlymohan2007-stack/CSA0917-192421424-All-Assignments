-- Task 2: doctor data used by DoctorLoadBench (database-per-change vs load-once Map)
CREATE DATABASE IF NOT EXISTS hospital;
USE hospital;

DROP TABLE IF EXISTS doctor;
CREATE TABLE doctor (
    doctor_id  INT AUTO_INCREMENT PRIMARY KEY,
    department VARCHAR(30) NOT NULL,
    name       VARCHAR(40) NOT NULL,
    INDEX idx_department (department)
);

INSERT INTO doctor (department, name) VALUES
    ('General Medicine', 'Dr. Rao'), ('General Medicine', 'Dr. Iyer'),
    ('Cardiology', 'Dr. Mehta'), ('Cardiology', 'Dr. Nair'), ('Cardiology', 'Dr. Khan'),
    ('Orthopedics', 'Dr. Das'),
    ('Pediatrics', 'Dr. Sen'), ('Pediatrics', 'Dr. Paul');
