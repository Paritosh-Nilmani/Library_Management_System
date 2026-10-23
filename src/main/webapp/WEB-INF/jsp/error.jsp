<%
    request.setAttribute("pageTitle", "Error");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>

<section class="panel">
    <div class="empty-state">
        <i class="bi bi-exclamation-triangle"></i>
        <h2>Something went wrong</h2>
        <p>Please check the server log for details, then try again.</p>
        <a class="btn btn-primary" href="<%= contextPath %>/dashboard">Back to Dashboard</a>
    </div>
</section>

<%@ include file="includes/footer.jsp" %>
