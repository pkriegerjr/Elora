package com.elora.common.util;

/**
 * Normalização/validação de documentos (espelha {@code CONFIG.normalizeCPF} do front).
 * CPF é gravado como CHAR(11) só-dígitos, como exige o schema v2.
 */
public final class DocumentUtils {

    private DocumentUtils() {
    }

    public static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    /**
     * Valida CPF com dígitos verificadores. Rejeita sequências repetidas
     * (ex.: 111.111.111-11) que passam na conta mas são inválidas.
     */
    public static boolean isValidCpf(String cpf) {
        String d = onlyDigits(cpf);
        if (d.length() != 11) {
            return false;
        }
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (d.charAt(i) - '0') * (10 - i);
            }
            int resto = soma % 11;
            int dv1 = resto < 2 ? 0 : 11 - resto;
            if (dv1 != d.charAt(9) - '0') {
                return false;
            }
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (d.charAt(i) - '0') * (11 - i);
            }
            resto = soma % 11;
            int dv2 = resto < 2 ? 0 : 11 - resto;
            return dv2 == d.charAt(10) - '0';
        } catch (Exception e) {
            return false;
        }
    }
}
