package com.elora.module.notificacao.service;

import com.elora.module.notificacao.entity.Notificacao;
import com.elora.module.notificacao.enums.Canal;
import com.elora.module.notificacao.repository.NotificacaoRepository;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.repository.UsuarioPerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Avisos automáticos do sistema (equipe interna, clientes e cuidadores).
 * Escreve direto na tabela {@code notificacao}, sem exigir staff — o remetente
 * é o próprio sistema, sempre com {@code canal = sistema}.
 *
 * <p>Fica isolado de {@link NotificacaoService} para evitar ciclo: serviços de
 * domínio (usuário, contrato, jurídico...) dependem desta classe, e
 * {@code NotificacaoService} já depende de {@code UsuarioService}.</p>
 */
@Service
@RequiredArgsConstructor
public class AvisoSistemaService {

    private static final Logger log = LoggerFactory.getLogger(AvisoSistemaService.class);

    public static final Set<String> ADMINISTRACAO = Set.of("admin", "moderador");
    public static final Set<String> JURIDICO = Set.of("admin", "juridico");
    public static final Set<String> FINANCEIRO = Set.of("admin", "financeiro");

    private final NotificacaoRepository notificacoes;
    private final UsuarioPerfilRepository usuarioPerfis;
    private final UsuarioRepository usuarios;

    /** Avisa todos os usuários que possuem algum dos perfis informados. */
    @Transactional
    public int notificar(Collection<String> perfis, String titulo, String corpo,
                         String referenciaTipo, Integer referenciaId) {
        if (perfis == null || perfis.isEmpty()) {
            return 0;
        }
        Set<Integer> ids = new LinkedHashSet<>();
        usuarioPerfis.findByPerfil_NomeIn(perfis).forEach(up -> ids.add(up.getUsuario().getId()));
        return notificarUsuarios(ids, titulo, corpo, referenciaTipo, referenciaId);
    }

    /** Avisa um único usuário final (cliente/cuidador). */
    @Transactional
    public int notificarUsuario(Integer usuarioId, String titulo, String corpo,
                                String referenciaTipo, Integer referenciaId) {
        if (usuarioId == null) {
            return 0;
        }
        return notificarUsuarios(Set.of(usuarioId), titulo, corpo, referenciaTipo, referenciaId);
    }

    /** Avisa um conjunto explícito de usuários, ignorando excluídos lógicos. */
    @Transactional
    public int notificarUsuarios(Collection<Integer> usuarioIds, String titulo, String corpo,
                                 String referenciaTipo, Integer referenciaId) {
        if (usuarioIds == null || usuarioIds.isEmpty() || titulo == null || titulo.isBlank()) {
            return 0;
        }
        String tituloFinal = titulo.trim().length() > 150 ? titulo.trim().substring(0, 150) : titulo.trim();
        int total = 0;
        for (Usuario destinatario : usuarios.findAllById(usuarioIds)) {
            if (destinatario.getDeletedAt() != null) {
                continue;
            }
            Notificacao n = new Notificacao();
            n.setDestinatario(destinatario);
            n.setCanal(Canal.sistema);
            n.setTitulo(tituloFinal);
            n.setCorpo(corpo == null ? "" : corpo);
            n.setReferenciaTipo(referenciaTipo);
            n.setReferenciaId(referenciaId);
            n.setLida(false);
            notificacoes.save(n);
            total++;
        }
        return total;
    }

    /** Aviso nunca impede a operação que o disparou. */
    public void notificarSeguro(Collection<String> perfis, String titulo, String corpo,
                                String referenciaTipo, Integer referenciaId) {
        seguro(() -> notificar(perfis, titulo, corpo, referenciaTipo, referenciaId), titulo);
    }

    /** Versão segura para um único destinatário. */
    public void notificarSeguroUsuario(Integer usuarioId, String titulo, String corpo,
                                       String referenciaTipo, Integer referenciaId) {
        seguro(() -> notificarUsuario(usuarioId, titulo, corpo, referenciaTipo, referenciaId), titulo);
    }

    /** Versão segura para vários destinatários. */
    public void notificarSeguroUsuarios(Collection<Integer> usuarioIds, String titulo, String corpo,
                                        String referenciaTipo, Integer referenciaId) {
        seguro(() -> notificarUsuarios(usuarioIds, titulo, corpo, referenciaTipo, referenciaId), titulo);
    }

    private void seguro(Runnable acao, String titulo) {
        try {
            acao.run();
        } catch (Exception e) {
            log.warn("Falha ao registrar aviso automático '{}': {}", titulo, e.toString());
        }
    }
}
