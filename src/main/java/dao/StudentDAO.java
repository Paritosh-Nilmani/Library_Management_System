package dao;

import model.Student;
import util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    private static final String BASE_SELECT = "SELECT student_id, roll_no, name, email, phone, department, course, enrollment_date, status FROM students";

    public boolean addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (roll_no, name, email, phone, department, course, enrollment_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStudentStatement(statement, student);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET roll_no = ?, name = ?, email = ?, phone = ?, department = ?, course = ?, enrollment_date = ?, status = ? "
                + "WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStudentStatement(statement, student);
            statement.setInt(9, student.getStudentId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteStudent(int studentId) throws SQLException {
        // Keep historic loans and payments linked to the student record.
        String sql = "UPDATE students SET status = 'INACTIVE' WHERE student_id = ? AND status <> 'INACTIVE'";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    public Student getStudentById(int studentId) throws SQLException {
        String sql = BASE_SELECT + " WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapStudent(resultSet);
                }
            }
        }

        return null;
    }

    public List<Student> getAllStudents() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY name";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapStudents(resultSet);
        }
    }

    public List<Student> getActiveStudents() throws SQLException {
        String sql = BASE_SELECT + " WHERE status = 'ACTIVE' ORDER BY name";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapStudents(resultSet);
        }
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        return searchStudents(keyword, 1, Integer.MAX_VALUE);
    }

    public List<Student> searchStudents(String keyword, int page, int pageSize) throws SQLException {
        boolean filtered = keyword != null && !keyword.trim().isEmpty();
        String sql = BASE_SELECT + (filtered ? " WHERE roll_no LIKE ? OR name LIKE ? OR email LIKE ? OR phone LIKE ? OR department LIKE ? OR course LIKE ?" : "")
                + " ORDER BY name LIMIT ? OFFSET ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (filtered) {
                String term = "%" + keyword.trim() + "%";
                for (int i = 0; i < 6; i++) statement.setString(index++, term);
            }
            statement.setInt(index++, pageSize);
            statement.setLong(index, (long) (page - 1) * pageSize);
            try (ResultSet resultSet = statement.executeQuery()) { return mapStudents(resultSet); }
        }
    }

    public int countSearchStudents(String keyword) throws SQLException {
        boolean filtered = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT COUNT(*) FROM students" + (filtered ? " WHERE roll_no LIKE ? OR name LIKE ? OR email LIKE ? OR phone LIKE ? OR department LIKE ? OR course LIKE ?" : "");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filtered) {
                String term = "%" + keyword.trim() + "%";
                for (int i = 1; i <= 6; i++) statement.setString(i, term);
            }
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getInt(1) : 0; }
        }
    }

    public int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }

        return 0;
    }

    public int countActiveStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE status = 'ACTIVE'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void fillStudentStatement(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getRollNo());
        statement.setString(2, student.getName());
        statement.setString(3, nullIfBlank(student.getEmail()));
        statement.setString(4, nullIfBlank(student.getPhone()));
        statement.setString(5, nullIfBlank(student.getDepartment()));
        statement.setString(6, nullIfBlank(student.getCourse()));
        if (student.getEnrollmentDate() == null) statement.setNull(7, java.sql.Types.DATE);
        else statement.setDate(7, Date.valueOf(student.getEnrollmentDate()));
        statement.setString(8, student.getStatus());
    }

    private List<Student> mapStudents(ResultSet resultSet) throws SQLException {
        List<Student> students = new ArrayList<>();
        while (resultSet.next()) {
            students.add(mapStudent(resultSet));
        }
        return students;
    }

    private Student mapStudent(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setStudentId(resultSet.getInt("student_id"));
        student.setRollNo(resultSet.getString("roll_no"));
        student.setName(resultSet.getString("name"));
        student.setEmail(resultSet.getString("email"));
        student.setPhone(resultSet.getString("phone"));
        student.setDepartment(resultSet.getString("department"));
        student.setCourse(resultSet.getString("course"));
        Date enrollmentDate = resultSet.getDate("enrollment_date");
        student.setEnrollmentDate(enrollmentDate == null ? null : enrollmentDate.toLocalDate());
        student.setStatus(resultSet.getString("status"));
        return student;
    }

    private String nullIfBlank(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
