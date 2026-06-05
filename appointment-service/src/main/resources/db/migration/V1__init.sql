-- Flyway migration for appointment-service
CREATE TABLE IF NOT EXISTS doctors (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200),
  specialization VARCHAR(200),
  phone VARCHAR(50),
  email VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS appointments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT,
  appointment_date DATE,
  appointment_time TIME,
  status VARCHAR(50) DEFAULT 'BOOKED',
  CONSTRAINT fk_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL,
  UNIQUE KEY ux_doctor_slot (doctor_id, appointment_date, appointment_time)
);
