package com.example.alphasync;

import static org.junit.Assert.*;

import com.example.alphasync.utils.ValidationUtils;

import org.junit.Test;

/** Local JVM unit tests for the validation rules in the requirements (section 11). */
public class ValidationUtilsTest {
    @Test public void email() {
        assertTrue(ValidationUtils.isValidEmail("tech@nhls.ac.za"));
        assertFalse(ValidationUtils.isValidEmail("tech@nhls"));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test public void nonNegativeInt() {
        assertEquals(Integer.valueOf(0), ValidationUtils.parseNonNegativeInt("0"));
        assertEquals(Integer.valueOf(25), ValidationUtils.parseNonNegativeInt(" 25 "));
        assertNull(ValidationUtils.parseNonNegativeInt("-1"));
        assertNull(ValidationUtils.parseNonNegativeInt("abc"));
        assertNull(ValidationUtils.parseNonNegativeInt("1.5"));
        assertNull(ValidationUtils.parseNonNegativeInt(""));
    }

    @Test public void dates() {
        assertTrue(ValidationUtils.isValidDate("2026-10-12"));
        assertFalse(ValidationUtils.isValidDate("2026-02-31"));
        assertFalse(ValidationUtils.isValidDate("12/10/2026"));
        assertFalse(ValidationUtils.isValidDate(""));
    }

    @Test public void inventoryIdIsStableAndSafe() {
        assertEquals("lab1_polyvalent-snake-antivenom",
                ValidationUtils.inventoryId("lab1", "  Polyvalent Snake Antivenom "));
        assertEquals(ValidationUtils.inventoryId("lab1", "SAIMR Scorpion"),
                ValidationUtils.inventoryId("lab1", "saimr   scorpion"));
    }
}
