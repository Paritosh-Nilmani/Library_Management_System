package servlet;

import dao.StudentDAO;
import dao.AuditDAO;
import dao.IssuedBookDAO;
import model.Student;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class StudentServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final StudentDAO studentDAO = new StudentDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final IssuedBookDAO issuedBookDAO = new IssuedBookDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        String action = value(request.getParameter("action"));
        try {
            if ("new".equals(action)) {
                Student student = new Student();
                student.setStatus("ACTIVE");
                showForm(request, response, student, "create");
            } else if ("edit".equals(action) || "view".equals(action)) {
                int studentId = Integer.parseInt(request.getParameter("id"));
                Student student = studentDAO.getStudentById(studentId);
                if (student == null) {
                    setFlash(request, "warning", "Student not found.");
                    response.sendRedirect(request.getContextPath() + "/students");
                    return;
                }
                if ("view".equals(action)) {
                    request.setAttribute("student", student);
                    request.setAttribute("loans", issuedBookDAO.getLoansForStudent(studentId));
                    request.getRequestDispatcher("/WEB-INF/jsp/student-profile.jsp").forward(request, response);
                } else {
                    showForm(request, response, student, "update");
                }
            } else {
                listStudents(request, response);
            }
        } catch (NumberFormatException exception) {
            setFlash(request, "danger", "Invalid student id.");
            response.sendRedirect(request.getContextPath() + "/students");
        } catch (SQLException exception) {
            throw new ServletException("Unable to process student request.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action = value(request.getParameter("action"));

        try {
            if ("create".equals(action)) {
                createStudent(request, response);
            } else if ("update".equals(action)) {
                updateStudent(request, response);
            } else if ("delete".equals(action)) {
                deleteStudent(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/students");
            }
        } catch (SQLException exception) {
            throw new ServletException("Unable to save student data.", exception);
        }
    }

    private void listStudents(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        String keyword = value(request.getParameter("keyword"));
        int pageSize = 20;
        int page = parsePage(request.getParameter("page"));
        int totalRows = studentDAO.countSearchStudents(keyword);
        int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
        page = Math.min(page, totalPages);
        request.setAttribute("students", studentDAO.searchStudents(keyword, page, pageSize));
        request.setAttribute("keyword", keyword);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.getRequestDispatcher("/WEB-INF/jsp/students.jsp").forward(request, response);
    }

    private int parsePage(String raw) {
        try { return Math.max(1, Integer.parseInt(raw)); } catch (RuntimeException ignored) { return 1; }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Student student, String formAction)
            throws ServletException, IOException {
        request.setAttribute("student", student);
        request.setAttribute("formAction", formAction);
        request.getRequestDispatcher("/WEB-INF/jsp/student-form.jsp").forward(request, response);
    }

    private void createStudent(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<String> errors = new ArrayList<>();
        Student student = readStudent(request, false, errors);

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            showForm(request, response, student, "create");
            return;
        }

        try {
            studentDAO.addStudent(student);
            auditDAO.record(currentAdmin(request).getAdminId(), "STUDENT_CREATED", "student", student.getRollNo(), student.getName());
            setFlash(request, "success", "Student added successfully.");
            response.sendRedirect(request.getContextPath() + "/students");
        } catch (SQLException exception) {
            errors.add(databaseMessage(exception, "Unable to add student."));
            request.setAttribute("errors", errors);
            showForm(request, response, student, "create");
        }
    }

    private void updateStudent(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<String> errors = new ArrayList<>();
        Student student = readStudent(request, true, errors);

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            showForm(request, response, student, "update");
            return;
        }

        try {
            if (studentDAO.updateStudent(student)) {
                auditDAO.record(currentAdmin(request).getAdminId(), "STUDENT_UPDATED", "student", Integer.toString(student.getStudentId()), student.getName());
                setFlash(request, "success", "Student updated successfully.");
            } else {
                setFlash(request, "warning", "Student not found.");
            }
            response.sendRedirect(request.getContextPath() + "/students");
        } catch (SQLException exception) {
            errors.add(databaseMessage(exception, "Unable to update student."));
            request.setAttribute("errors", errors);
            showForm(request, response, student, "update");
        }
    }

    private void deleteStudent(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        List<String> errors = new ArrayList<>();
        int studentId = parseInteger(request.getParameter("studentId"), "Student ID", errors);

        if (!errors.isEmpty()) {
            setFlash(request, "danger", errors.get(0));
            response.sendRedirect(request.getContextPath() + "/students");
            return;
        }

        try {
            if (studentDAO.deleteStudent(studentId)) {
                auditDAO.record(currentAdmin(request).getAdminId(), "STUDENT_DEACTIVATED", "student", Integer.toString(studentId), "Student account deactivated");
                setFlash(request, "success", "Student deleted successfully.");
            } else {
                setFlash(request, "warning", "Student not found.");
            }
        } catch (SQLException exception) {
            setFlash(request, "danger", databaseMessage(exception, "Unable to delete student."));
        }

        response.sendRedirect(request.getContextPath() + "/students");
    }

    private Student readStudent(HttpServletRequest request, boolean requireId, List<String> errors) {
        Student student = new Student();
        if (requireId) {
            student.setStudentId(parseInteger(request.getParameter("studentId"), "Student ID", errors));
        }

        student.setRollNo(value(request.getParameter("rollNo")));
        student.setName(value(request.getParameter("name")));
        student.setEmail(value(request.getParameter("email")));
        student.setPhone(value(request.getParameter("phone")));
        student.setDepartment(value(request.getParameter("department")));
        student.setCourse(value(request.getParameter("course")));
        if (!isBlank(request.getParameter("enrollmentDate"))) {
            try { student.setEnrollmentDate(LocalDate.parse(request.getParameter("enrollmentDate"))); }
            catch (DateTimeParseException exception) { errors.add("Enrollment date must be a valid date."); }
        }
        student.setStatus(value(request.getParameter("status")));

        validateStudent(student, errors);
        return student;
    }

    private void validateStudent(Student student, List<String> errors) {
        if (isBlank(student.getRollNo())) {
            errors.add("Roll number is required.");
        }
        if (isBlank(student.getName())) {
            errors.add("Student name is required.");
        }
        if (!isBlank(student.getEmail()) && !student.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errors.add("Email address is invalid.");
        }
        if (!isBlank(student.getPhone()) && !student.getPhone().matches("^[0-9+\\- ]{7,20}$")) {
            errors.add("Phone number is invalid.");
        }
        if (student.getRollNo().length() > 50 || student.getName().length() > 120
                || student.getEmail().length() > 120 || student.getPhone().length() > 20
                || student.getDepartment().length() > 80 || student.getCourse().length() > 100) {
            errors.add("One or more student fields exceed the permitted length.");
        }
        if (!"ACTIVE".equals(student.getStatus()) && !"INACTIVE".equals(student.getStatus())) {
            errors.add("Status must be ACTIVE or INACTIVE.");
        }
    }

    private String databaseMessage(SQLException exception, String fallback) {
        if ("23000".equals(exception.getSQLState())) {
            return fallback + " Check duplicate roll numbers or existing issue history.";
        }
        return fallback + " Check for duplicate registration numbers or existing loan history.";
    }
}
