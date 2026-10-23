<%@ page import="java.util.List" %>
<%@ page import="util.HtmlUtil" %>
<%
    String flashMessage = (String) session.getAttribute("flashMessage");
    String flashType = (String) session.getAttribute("flashType");
    if (flashMessage != null) {
        if (flashType == null) {
            flashType = "info";
        }
%>
    <div class="alert alert-<%= HtmlUtil.escape(flashType) %> alert-dismissible fade show" role="alert">
        <%= HtmlUtil.escape(flashMessage) %>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
<%
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
    }

    List<String> errors = (List<String>) request.getAttribute("errors");
    if (errors != null && !errors.isEmpty()) {
%>
    <div class="alert alert-danger" role="alert">
        <strong>Please fix the following:</strong>
        <ul class="mb-0 mt-2">
            <% for (String error : errors) { %>
                <li><%= HtmlUtil.escape(error) %></li>
            <% } %>
        </ul>
    </div>
<%
    }
%>
