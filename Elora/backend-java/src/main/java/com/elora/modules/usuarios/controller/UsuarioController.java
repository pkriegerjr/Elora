package com.elora.modules.usuarios.controller;

import com.elora.modules.usuarios.dto.CreateUsuarioExternoRequest;
import com.elora.modules.usuarios.dto.CreateUsuarioInternoRequest;
import com.elora.modules.usuarios.service.UsuarioService;
import io.javalin.http.Context;

public class UsuarioController {

    public static void listInternos(Context ctx) {
        try { ctx.json(UsuarioService.listInternos()); }
        catch (Exception e) { ctx.status(500).json(java.util.Map.of("error", e.getMessage())); }
    }

    public static void listExternos(Context ctx) {
        try { ctx.json(UsuarioService.listExternos()); }
        catch (Exception e) { ctx.status(500).json(java.util.Map.of("error", e.getMessage())); }
    }

    public static void getInternoById(Context ctx) {
        ctx.json(java.util.Map.of("todo", "implementar SELECT por id"));
    }

    public static void getExternoById(Context ctx) {
        ctx.json(java.util.Map.of("todo", "implementar SELECT por id"));
    }

    public static void createInterno(Context ctx) {
        try {
            var req = ctx.bodyAsClass(CreateUsuarioInternoRequest.class);
            ctx.status(201).json(UsuarioService.createInterno(req));
        } catch (Exception e) {
            ctx.status(400).json(java.util.Map.of("error", e.getMessage()));
        }
    }

    public static void createExterno(Context ctx) {
        try {
            var req = ctx.bodyAsClass(CreateUsuarioExternoRequest.class);
            ctx.status(201).json(UsuarioService.createExterno(req));
        } catch (Exception e) {
            ctx.status(400).json(java.util.Map.of("error", e.getMessage()));
        }
    }
}
