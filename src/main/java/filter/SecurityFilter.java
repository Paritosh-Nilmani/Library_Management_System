package filter;

import model.Admin;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Applies route protection, CSRF checks, UTF-8, and baseline browser security headers. */
public class SecurityFilter implements Filter {
    private static final String TOKEN_ATTRIBUTE = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public void init(FilterConfig filterConfig) { }
    @Override public void destroy() { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; "
                + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                + "font-src 'self' data: https://cdn.jsdelivr.net; connect-src 'self'; frame-ancestors 'none'");

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!isPublicPath(path)) {
            HttpSession session = request.getSession(false);
            Admin admin = session == null ? null : (Admin) session.getAttribute("admin");
            if (admin == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        }

        HttpSession session = request.getSession(!isSafeMethod(request.getMethod()) || "/login".equals(path));
        if (session != null) {
            String token = (String) session.getAttribute(TOKEN_ATTRIBUTE);
            if (token == null) {
                token = newToken();
                session.setAttribute(TOKEN_ATTRIBUTE, token);
            }
            request.setAttribute(TOKEN_ATTRIBUTE, token);
            if (!isSafeMethod(request.getMethod())) {
                String submitted = request.getParameter(TOKEN_ATTRIBUTE);
                if (submitted == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "The request token is invalid or expired.");
                    return;
                }
            }
        } else if (!isSafeMethod(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return "/login".equals(path) || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || "/favicon.ico".equals(path);
    }

    private boolean isSafeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
