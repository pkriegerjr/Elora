package com.elora.modules.usuarios.service;

import com.elora.modules.usuarios.dto.CreateUsuarioExternoRequest;
import com.elora.modules.usuarios.dto.CreateUsuarioInternoRequest;
import com.elora.modules.usuarios.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioService {

    private static final UsuarioRepository repo = new UsuarioRepository();

    public static Object listInternos() throws Exception {
        return repo.findAllInternos();
    }

    public static Object listExternos() throws Exception {
        return repo.findAllExternos();
    }

    public static Object createInterno(CreateUsuarioInternoRequest req) throws Exception {
        String hash = null;
        if ("senha".equals(req.origemLogin)) {
            if (req.senha == null) throw new IllegalArgumentException("senha obrigatória para origem_login=senha (CHECK chk_login_senha)");
            hash = BCrypt.hashpw(req.senha, BCrypt.gensalt(12));
        }
        int id = repo.insertInterno(req.nome, req.email, hash, req.origemLogin, req.tipoPerfil);
        return java.util.Map.of("id", id, "message", "usuario_interno criado");
    }

    public static Object createExterno(CreateUsuarioExternoRequest req) throws Exception {
        String hash = null;
        if ("senha".equals(req.origemLogin)) {
            if (req.senha == null) throw new IllegalArgumentException("senha obrigatória");
            hash = BCrypt.hashpw(req.senha, BCrypt.gensalt(12));
        }
        int id = repo.insertExterno(req.nome, req.email, hash, req.origemLogin, req.tipoPerfil, req.telefone);
        return java.util.Map.of("id", id, "message", "usuario_externo criado");
    }
}
