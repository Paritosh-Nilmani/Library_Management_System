<%@ page import="model.Admin" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Admin Profile");
    Admin admin = (Admin) request.getAttribute("admin");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel form-panel">
    <div class="panel-header">
        <div>
            <h2>Account details</h2>
            <p>Signed in as <strong><%= HtmlUtil.escape(admin.getFullName()) %></strong> (<%= HtmlUtil.escape(admin.getUsername()) %>).</p>
        </div>
    </div>
    <h3 class="h5 mt-4">Change password</h3>
    <form action="<%= contextPath %>/profile" method="post" class="row g-3 needs-validation" novalidate>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <div class="col-12">
            <label for="currentPassword" class="form-label">Current password</label>
            <input type="password" class="form-control" id="currentPassword" name="currentPassword" autocomplete="current-password" required>
        </div>
        <div class="col-md-6">
            <label for="newPassword" class="form-label">New password</label>
            <input type="password" class="form-control" id="newPassword" name="newPassword" minlength="12" autocomplete="new-password" required>
            <div class="form-text">Use 12–72 UTF-8 bytes.</div>
        </div>
        <div class="col-md-6">
            <label for="confirmPassword" class="form-label">Confirm new password</label>
            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" minlength="12" autocomplete="new-password" required>
        </div>
        <div class="col-12 form-actions">
            <button type="submit" class="btn btn-primary"><i class="bi bi-key"></i> Update password</button>
        </div>
    </form>
</section>

<%@ include file="includes/footer.jsp" %>
