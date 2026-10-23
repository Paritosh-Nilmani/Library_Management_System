-- Optional, idempotent sample catalog and students for a fresh local/demo database.
-- Creates no admin account and contains no production credentials.
USE library_management_system;

INSERT INTO books (title, author, publisher, category, isbn, publication_year, total_copies, available_copies, shelf_location)
VALUES
  ('Clean Code', 'Robert C. Martin', 'Prentice Hall', 'Software Engineering', '9780132350884', 2008, 5, 5, 'A1'),
  ('Effective Java', 'Joshua Bloch', 'Addison-Wesley', 'Programming', '9780134685991', 2018, 4, 4, 'A2'),
  ('Database System Concepts', 'Abraham Silberschatz', 'McGraw-Hill', 'Databases', '9780073523323', 2010, 3, 3, 'B1')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO students (roll_no, name, email, phone, department, course, enrollment_date, status)
VALUES
  ('DEMO-001', 'Ananya Sharma', 'ananya.demo@example.com', '9000000001', 'Computer Science', 'BSc Computer Science', '2024-08-01', 'ACTIVE'),
  ('DEMO-002', 'Rahul Mehta', 'rahul.demo@example.com', '9000000002', 'Information Technology', 'BSc Information Technology', '2024-08-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);
