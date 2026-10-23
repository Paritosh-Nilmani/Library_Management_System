<%@ page import="model.Student" %>
<%@ page import="util.HtmlUtil" %>
<%
    Student student = (Student) request.getAttribute("student");
    if (student == null) {
        student = new Student();
        student.setStatus("ACTIVE");
    }
    String formAction = (String) request.getAttribute("formAction");
    boolean editing = "update".equals(formAction);
    request.setAttribute("pageTitle", editing ? "Update Student" : "Add Student");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel form-panel">
    <div class="panel-header">
        <div>
            <h2><%= editing ? "Update Student Details" : "Add New Student" %></h2>
            <p>Student status controls whether they can receive newly issued books.</p>
        </div>
    </div>

    <form action="<%= contextPath %>/students" method="post" class="row g-3 needs-validation" novalidate>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="action" value="<%= HtmlUtil.escape(formAction) %>">
        <% if (editing) { %>
            <input type="hidden" name="studentId" value="<%= student.getStudentId() %>">
        <% } %>

        <div class="col-md-6">
            <label for="rollNo" class="form-label">Roll Number</label>
            <input type="text" class="form-control" id="rollNo" name="rollNo"
                   value="<%= HtmlUtil.escape(student.getRollNo()) %>" maxlength="50" required>
            <div class="invalid-feedback">Roll number is required.</div>
        </div>

        <div class="col-md-6">
            <label for="name" class="form-label">Name</label>
            <input type="text" class="form-control" id="name" name="name"
                   value="<%= HtmlUtil.escape(student.getName()) %>" maxlength="120" required>
            <div class="invalid-feedback">Student name is required.</div>
        </div>

        <div class="col-md-6">
            <label for="email" class="form-label">Email</label>
            <input type="email" class="form-control" id="email" name="email"
                   value="<%= HtmlUtil.escape(student.getEmail()) %>" maxlength="120">
            <div class="invalid-feedback">Enter a valid email address.</div>
        </div>

        <div class="col-md-6">
            <label for="phone" class="form-label">Phone</label>
            <input type="text" class="form-control" id="phone" name="phone"
                   value="<%= HtmlUtil.escape(student.getPhone()) %>" maxlength="20" pattern="[0-9+\- ]{7,20}">
            <div class="invalid-feedback">Enter a valid phone number.</div>
        </div>

        <div class="col-md-6">
            <label for="department" class="form-label">Department</label>
            <input type="text" class="form-control" id="department" name="department"
                   value="<%= HtmlUtil.escape(student.getDepartment()) %>" maxlength="80">
        </div>

        <div class="col-md-6">
            <label for="course" class="form-label">Course</label>
            <input type="text" class="form-control" id="course" name="course"
                   value="<%= HtmlUtil.escape(student.getCourse()) %>" maxlength="100">
        </div>

        <div class="col-md-6">
            <label for="enrollmentDate" class="form-label">Enrollment Date</label>
            <input type="date" class="form-control" id="enrollmentDate" name="enrollmentDate"
                   value="<%= student.getEnrollmentDate() == null ? "" : student.getEnrollmentDate() %>">
        </div>

        <div class="col-md-6">
            <label for="status" class="form-label">Status</label>
            <select class="form-select" id="status" name="status" required>
                <option value="ACTIVE" <%= "ACTIVE".equals(student.getStatus()) ? "selected" : "" %>>ACTIVE</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(student.getStatus()) ? "selected" : "" %>>INACTIVE</option>
            </select>
        </div>

        <div class="col-12 form-actions">
            <a class="btn btn-outline-secondary" href="<%= contextPath %>/students">
                <i class="bi bi-arrow-left"></i> Cancel
            </a>
            <button type="submit" class="btn btn-primary">
                <i class="bi bi-save"></i> <%= editing ? "Update Student" : "Save Student" %>
            </button>
        </div>
    </form>
</section>

<%@ include file="includes/footer.jsp" %>
