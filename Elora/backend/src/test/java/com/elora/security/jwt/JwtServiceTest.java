package com.elora.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService("segredo-dev-elora-testes-32bytes!!", 60000);
    }

    @Test
    void geraEValida() {
        String token = jwtService.generate("42", Map.of("email", "a@b.c", "perfis", List.of("cliente")));

        assertNotNull(token);
        assertTrue(jwtService.valid(token));
        assertEquals("42", jwtService.parse(token).getSubject());
        assertEquals("a@b.c", jwtService.parse(token).get("email"));
    }

    @Test
    void rejeitaTokenRuim() {
        assertFalse(jwtService.valid("lixo"));
        assertFalse(jwtService.valid("a.b.c"));
    }
}
