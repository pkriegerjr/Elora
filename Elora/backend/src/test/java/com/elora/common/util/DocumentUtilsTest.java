package com.elora.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentUtilsTest {

    @Test
    void removeMascara() {
        assertEquals("52998224725", DocumentUtils.onlyDigits("529.982.247-25"));
        assertEquals("", DocumentUtils.onlyDigits(null));
    }

    @Test
    void cpfValido() {
        assertTrue(DocumentUtils.isValidCpf("52998224725"));
        assertTrue(DocumentUtils.isValidCpf("529.982.247-25"));
        assertTrue(DocumentUtils.isValidCpf("11144477735"));
    }

    @Test
    void cpfInvalido() {
        assertFalse(DocumentUtils.isValidCpf("11111111111"));
        assertFalse(DocumentUtils.isValidCpf("12345678901"));
        assertFalse(DocumentUtils.isValidCpf("123"));
        assertFalse(DocumentUtils.isValidCpf(null));
        assertFalse(DocumentUtils.isValidCpf("52998224726")); // dígito errado
    }
}
