package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsbnValidatorTest {
    @Test
    void acceptsValidIsbn10And13() {
        assertTrue(IsbnValidator.isValidOrBlank("0-13-235088-2"));
        assertTrue(IsbnValidator.isValidOrBlank("9780132350884"));
        assertTrue(IsbnValidator.isValidOrBlank(""));
    }

    @Test
    void rejectsInvalidChecksumsAndLengths() {
        assertFalse(IsbnValidator.isValidOrBlank("9780132350885"));
        assertFalse(IsbnValidator.isValidOrBlank("123"));
    }
}
