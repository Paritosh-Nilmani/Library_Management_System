<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="util.HtmlUtil" %>
<%
    String contextPath = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String username = (String) request.getAttribute("username");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login | Library Management System</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="<%= contextPath %>/css/style.css" rel="stylesheet">
</head>
<body class="login-body">
    <main class="login-shell">
        <section class="login-card">
            <div class="login-brand mb-4">
                <div class="brand-mark"><i class="bi bi-book-half"></i></div>
                <div>
                    <h1>Library Admin</h1>
                    <p>Sign in to manage books, students, and issues.</p>
                </div>
            </div>

            <% if (error != null) { %>
                <div class="alert alert-danger"><%= HtmlUtil.escape(error) %></div>
            <% } %>

            <form action="<%= contextPath %>/login" method="post" class="needs-validation" novalidate>
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <div class="mb-3">
                    <label for="username" class="form-label">Username</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-person"></i></span>
                        <input type="text" class="form-control" id="username" name="username"
                               value="<%= HtmlUtil.escape(username) %>" required autofocus>
                        <div class="invalid-feedback">Username is required.</div>
                    </div>
                </div>

                <div class="mb-4">
                    <label for="password" class="form-label">Password</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-lock"></i></span>
                        <input type="password" class="form-control" id="password" name="password" required>
                        <div class="invalid-feedback">Password is required.</div>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary w-100">
                    <i class="bi bi-box-arrow-in-right"></i> Login
                </button>
            </form>
        </section>
    </main>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        (() => {
            const forms = document.querySelectorAll('.needs-validation');
            Array.from(forms).forEach(form => {
                form.addEventListener('submit', event => {
                    if (!form.checkValidity()) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated');
                }, false);
            });
        })();
    </script>
</body>
</html>
