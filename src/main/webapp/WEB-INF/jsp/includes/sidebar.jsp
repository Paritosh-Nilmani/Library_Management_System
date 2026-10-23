<%@ page import="util.HtmlUtil" %>
<%
    String requestUri = request.getRequestURI();
    String adminName = loggedAdmin == null ? "Admin" : HtmlUtil.escape(loggedAdmin.getFullName());
%>
<div class="app-shell">
    <aside class="sidebar">
        <div class="sidebar-brand">
            <div class="brand-mark"><i class="bi bi-book-half"></i></div>
            <div>
                <div class="brand-title">LMS</div>
                <div class="brand-subtitle">Library Admin</div>
            </div>
        </div>

        <nav class="sidebar-nav">
            <a class="nav-link <%= requestUri.endsWith("/dashboard") ? "active" : "" %>" href="<%= contextPath %>/dashboard">
                <i class="bi bi-speedometer2"></i><span>Dashboard</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/books") ? "active" : "" %>" href="<%= contextPath %>/books">
                <i class="bi bi-journal-bookmark"></i><span>Books</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/students") ? "active" : "" %>" href="<%= contextPath %>/students">
                <i class="bi bi-people"></i><span>Students</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/issue-book") ? "active" : "" %>" href="<%= contextPath %>/issue-book">
                <i class="bi bi-box-arrow-up-right"></i><span>Issue Book</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/issued-books") ? "active" : "" %>" href="<%= contextPath %>/issued-books">
                <i class="bi bi-clipboard-check"></i><span>Issued Books</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/profile") ? "active" : "" %>" href="<%= contextPath %>/profile">
                <i class="bi bi-person-gear"></i><span>My Profile</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/payments") ? "active" : "" %>" href="<%= contextPath %>/payments">
                <i class="bi bi-cash-stack"></i><span>Fine Payments</span>
            </a>
            <a class="nav-link <%= requestUri.endsWith("/settings") ? "active" : "" %>" href="<%= contextPath %>/settings">
                <i class="bi bi-sliders"></i><span>Settings</span>
            </a>
        </nav>

        <div class="sidebar-footer">
            <div class="admin-chip">
                <i class="bi bi-person-circle"></i>
                <span><%= adminName %></span>
            </div>
            <form action="<%= contextPath %>/logout" method="post" class="mt-3">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <button type="submit" class="btn btn-outline-light btn-sm w-100">
                    <i class="bi bi-box-arrow-right"></i> Logout
                </button>
            </form>
        </div>
    </aside>

    <div class="main-panel">
        <header class="topbar">
            <div>
                <p class="eyebrow mb-1">Library Management System</p>
                <h1 class="page-title mb-0"><%= HtmlUtil.escape(pageTitle) %></h1>
            </div>
        </header>
        <main class="content">
