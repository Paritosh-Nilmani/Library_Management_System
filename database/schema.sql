-- Safe for a fresh database or repeat execution: no table or user data is dropped.
CREATE DATABASE IF NOT EXISTS library_management_system
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE library_management_system;

CREATE TABLE IF NOT EXISTS admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'LIBRARIAN') NOT NULL DEFAULT 'ADMIN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_changed_at TIMESTAMP NULL,
    CONSTRAINT chk_admin_username CHECK (CHAR_LENGTH(TRIM(username)) > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    author VARCHAR(100) NOT NULL,
    publisher VARCHAR(100),
    category VARCHAR(80),
    isbn VARCHAR(20) UNIQUE,
    publication_year SMALLINT UNSIGNED,
    total_copies INT UNSIGNED NOT NULL DEFAULT 1,
    available_copies INT UNSIGNED NOT NULL DEFAULT 1,
    shelf_location VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_books_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_books_author CHECK (CHAR_LENGTH(TRIM(author)) > 0),
    CONSTRAINT chk_books_total_copies CHECK (total_copies >= 0),
    CONSTRAINT chk_books_available_copies CHECK (available_copies <= total_copies),
    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_category (category),
    INDEX idx_books_publisher (publisher),
    INDEX idx_books_year (publication_year)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    roll_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120),
    phone VARCHAR(20),
    department VARCHAR(80),
    course VARCHAR(100),
    enrollment_date DATE,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_student_roll CHECK (CHAR_LENGTH(TRIM(roll_no)) > 0),
    CONSTRAINT chk_student_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX idx_students_name (name),
    INDEX idx_students_status (status),
    INDEX idx_students_department (department)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS library_settings (
    settings_id TINYINT UNSIGNED PRIMARY KEY,
    max_active_loans SMALLINT UNSIGNED NOT NULL DEFAULT 5,
    loan_period_days SMALLINT UNSIGNED NOT NULL DEFAULT 14,
    daily_fine DECIMAL(8,2) NOT NULL DEFAULT 5.00,
    max_fine DECIMAL(8,2) NOT NULL DEFAULT 500.00,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_library_settings_singleton CHECK (settings_id = 1),
    CONSTRAINT chk_library_settings_loans CHECK (max_active_loans > 0),
    CONSTRAINT chk_library_settings_days CHECK (loan_period_days > 0),
    CONSTRAINT chk_library_settings_fines CHECK (daily_fine >= 0 AND max_fine >= 0)
) ENGINE=InnoDB;
INSERT INTO library_settings (settings_id) VALUES (1)
ON DUPLICATE KEY UPDATE settings_id = VALUES(settings_id);

CREATE TABLE IF NOT EXISTS issued_books (
    issue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    student_id INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status ENUM('ISSUED', 'RETURNED', 'LOST', 'DAMAGED') NOT NULL DEFAULT 'ISSUED',
    fine DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    damage_note VARCHAR(500),
    issued_by INT,
    returned_by INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_issued_date_range CHECK (due_date >= issue_date),
    CONSTRAINT chk_issued_fine CHECK (fine >= 0),
    CONSTRAINT fk_issued_book FOREIGN KEY (book_id) REFERENCES books(book_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_issued_student FOREIGN KEY (student_id) REFERENCES students(student_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_issued_by FOREIGN KEY (issued_by) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_returned_by FOREIGN KEY (returned_by) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_issued_status_due (status, due_date),
    INDEX idx_issued_student_status (student_id, status),
    INDEX idx_issued_book_status (book_id, status),
    INDEX idx_issued_date (issue_date),
    INDEX idx_issued_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fine_payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    amount DECIMAL(8,2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER') NOT NULL DEFAULT 'CASH',
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by INT,
    note VARCHAR(255),
    CONSTRAINT chk_fine_payment_amount CHECK (amount > 0),
    CONSTRAINT fk_payment_issue FOREIGN KEY (issue_id) REFERENCES issued_books(issue_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_payment_admin FOREIGN KEY (recorded_by) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_fine_payments_issue (issue_id),
    INDEX idx_fine_payments_paid_at (paid_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id INT,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB;
