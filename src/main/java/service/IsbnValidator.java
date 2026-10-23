package service;

import java.util.Locale;

public final class IsbnValidator {
    private IsbnValidator() { }

    public static boolean isValidOrBlank(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) return true;
        String value = isbn.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
        if (value.matches("[0-9]{13}")) {
            int sum = 0;
            for (int i = 0; i < 13; i++) sum += (value.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
            return sum % 10 == 0;
        }
        if (!value.matches("[0-9]{9}[0-9X]")) return false;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = value.charAt(i) == 'X' ? 10 : value.charAt(i) - '0';
            sum += digit * (10 - i);
        }
        return sum % 11 == 0;
    }
}
