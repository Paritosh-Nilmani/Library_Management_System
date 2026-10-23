-- One-time migration from the original project schema. Back up the database first.
-- Legacy plaintext admin credentials are deliberately discarded; create/reset the
-- first admin afterward with util.AdminBootstrap.
USE library_management_system;

ALTER TABLE admin
    ADD COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN role ENUM('ADMIN', 'LIBRARIAN') NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN password_changed_at TIMESTAMP NULL;
UPDATE admin SET active = FALSE;
ALTER TABLE admin DROP COLUMN password;

ALTER TABLE issued_books
    DROP FOREIGN KEY fk_issued_books_book,
    DROP FOREIGN KEY fk_issued_books_student;

ALTER TABLE admin MODIFY admin_id INT NOT NULL AUTO_INCREMENT;
ALTER TABLE books MODIFY book_id INT NOT NULL AUTO_INCREMENT;
ALTER TABLE students MODIFY student_id INT NOT NULL AUTO_INCREMENT;
ALTER TABLE issued_books
    MODIFY issue_id BIGINT NOT NULL AUTO_INCREMENT,
    MODIFY book_id INT NOT NULL,
    MODIFY student_id INT NOT NULL;

ALTER TABLE books ADD COLUMN publication_year SMALLINT UNSIGNED NULL;
ALTER TABLE students
    ADD COLUMN course VARCHAR(100) NULL,
    ADD COLUMN enrollment_date DATE NULL;
ALTER TABLE issued_books
    MODIFY status ENUM('ISSUED', 'RETURNED', 'LOST', 'DAMAGED') NOT NULL DEFAULT 'ISSUED',
    ADD COLUMN damage_note VARCHAR(500) NULL,
    ADD COLUMN issued_by INT NULL,
    ADD COLUMN returned_by INT NULL,
    ADD INDEX idx_issued_student_status (student_id, status),
    ADD INDEX idx_issued_book_status (book_id, status),
    ADD CONSTRAINT fk_issued_book FOREIGN KEY (book_id) REFERENCES books(book_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_issued_student FOREIGN KEY (student_id) REFERENCES students(student_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_issued_by FOREIGN KEY (issued_by) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT fk_returned_by FOREIGN KEY (returned_by) REFERENCES admin(admin_id)
        ON UPDATE CASCADE ON DELETE SET NULL;

-- Run database/schema.sql after this migration to create settings, payment, and audit tables.
