<%
    if (session.getAttribute("admin") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
    } else {
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
%>
