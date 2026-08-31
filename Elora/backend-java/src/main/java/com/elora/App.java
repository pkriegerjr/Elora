package com.elora;

import com.elora.config.DatabaseConfig;
import com.elora.modules.usuarios.controller.UsuarioController;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class App {
    public static void main(String[] args) {
        DatabaseConfig.init();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.http.defaultContentType = "application/json";
        });

        // Healthcheck
        app.get("/", ctx -> ctx.json(java.util.Map.of("status", "Elora API Java/Javalin rodando", "version", "1.0.0")));

        // Módulo Usuários - mapeia usuario_interno + usuario_externo do v1
        app.get("/api/usuarios/internos", UsuarioController::listInternos);
        app.get("/api/usuarios/externos", UsuarioController::listExternos);
        app.get("/api/usuarios/internos/{id}", UsuarioController::getInternoById);
        app.get("/api/usuarios/externos/{id}", UsuarioController::getExternoById);
        app.post("/api/usuarios/internos", UsuarioController::createInterno);
        app.post("/api/usuarios/externos", UsuarioController::createExterno);

        // 404 handler
        app.error(HttpStatus.NOT_FOUND.getCode(), ctx -> ctx.json(java.util.Map.of("error", "Rota não encontrada")));

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7001"));
        app.start(port);
        System.out.println("Elora Javalin rodando em http://localhost:" + port);
    }
}
