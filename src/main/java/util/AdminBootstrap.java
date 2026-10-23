package util;

import dao.AdminDAO;

import java.io.Console;
import java.nio.charset.StandardCharsets;

/** Creates the first administrator without storing a demo credential in source or SQL. */
public final class AdminBootstrap {
    private AdminBootstrap() { }

    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run this command from a terminal that supports secure password input.");
        String username = console.readLine("Admin username: ").trim();
        String fullName = console.readLine("Admin display name: ").trim();
        char[] passwordChars = console.readPassword("Admin password (12-72 UTF-8 bytes): ");
        try {
            String password = new String(passwordChars);
            int length = password.getBytes(StandardCharsets.UTF_8).length;
            if (username.isEmpty() || username.length() > 50 || fullName.isEmpty() || fullName.length() > 100
                    || length < 12 || length > 72) {
                throw new IllegalArgumentException("Username, display name, or password length is invalid.");
            }
            boolean created = new AdminDAO().createFirstAdmin(username, fullName, password);
            System.out.println(created ? "Initial admin account created." : "An admin account already exists; no account was created.");
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }
}
