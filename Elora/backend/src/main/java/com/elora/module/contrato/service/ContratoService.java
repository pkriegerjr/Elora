package com.elora.module.contrato.service;

import com.elora.common.audit.AuditoriaService;
import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.dto.AssinaturaResponse;
import com.elora.module.contrato.dto.AtualizarContratoRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.entity.AssinaturaContrato;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.repository.AssinaturaContratoRepository;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REQ-008/009: ciclo de vida do contrato + assinatura digital simplificada.
 * Funil: rascunho → proposta → negociacao → aguard_assinatura → ativo →
 * concluido; desvios: cancelado, rescindido, em_disputa. Quando cliente E
 * profissional assinam em aguard_assinatura, o contrato ativa sozinho —
 * destravando o pagamento (REQ-007).
 */
@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratos;
    private final AssinaturaContratoRepository assinaturas;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;

    private static final Set<StatusContrato> FINAIS =
            Set.of(StatusContrato.concluido, StatusContrato.rescindido, StatusContrato.cancelado);

    private static final Map<StatusContrato, Set<StatusContrato>> TRANSICOES = new EnumMap<>(StatusContrato.class);

    static {
        TRANSICOES.put(StatusContrato.rascunho, Set.of(StatusContrato.proposta, StatusContrato.cancelado));
        TRANSICOES.put(StatusContrato.proposta,
                Set.of(StatusContrato.negociacao, StatusContrato.aguard_assinatura, StatusContrato.cancelado));
        TRANSICOES.put(StatusContrato.negociacao,
                Set.of(StatusContrato.aguard_assinatura, StatusContrato.cancelado));
        TRANSICOES.put(StatusContrato.aguard_assinatura,
                Set.of(StatusContrato.ativo, StatusContrato.cancelado));
        TRANSICOES.put(StatusContrato.ativo,
                Set.of(StatusContrato.concluido, StatusContrato.em_disputa, StatusContrato.rescindido));
        TRANSICOES.put(StatusContrato.em_disputa,
                Set.of(StatusContrato.ativo, StatusContrato.concluido, StatusContrato.rescindido));
    }

    /** Cria sempre em rascunho, com código único gerado na app. */
    @Transactional
    public ContratoResponse criar(Integer viewerId, CriarContratoRequest req, String ip) {
        Usuario cliente = usuarioService.getVisivel(req.getClienteId());
        Usuario profissional = usuarioService.getVisivel(req.getProfissionalId());
        if (!cliente.getId().equals(viewerId) && !usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente o próprio cliente ou a equipe Elora pode criar o contrato");
        }
        if (cliente.getId().equals(profissional.getId())) {
            throw new BusinessException("Cliente e profissional não podem ser o mesmo usuário");
        }
        if (!usuarioService.perfisDe(profissional.getId()).contains("profissional")) {
            throw new BusinessException("O usuário contratado não possui perfil de profissional");
        }
        validarDatas(req.getDataInicio(), req.getDataFim());

        Contrato contrato = new Contrato();
        contrato.setCodigo(gerarCodigoUnico());
        contrato.setCliente(cliente);
        contrato.setProfissional(profissional);
        contrato.setTitulo(req.getTitulo().trim());
        contrato.setDescricaoNecessidade(vazioParaNull(req.getDescricaoNecessidade()));
        contrato.setValorHora(req.getValorHora());
        contrato.setValorTotal(req.getValorTotal());
        contrato.setEnderecoAtendimento(vazioParaNull(req.getEnderecoAtendimento()));
        contrato.setDataInicio(req.getDataInicio());
        contrato.setDataFim(req.getDataFim());
        contrato.setStatus(StatusContrato.rascunho);
        contrato.setCriadoPor(usuarioService.getVisivel(viewerId));
        Contrato salvo = contratos.save(contrato);
        auditoria.registrar(viewerId, "contrato.criar", "contrato", salvo.getId(), ip);
        notificacoes.notificarSistema(profissional.getId(), "Nova proposta de contrato",
                cliente.getNome() + " propôs: " + salvo.getTitulo() + " (" + salvo.getCodigo() + ")",
                "contrato", salvo.getId());
        return mapear(salvo);
    }

    /** Meus contratos (papel cliente/profissional/os dois, mais novos antes). */
    @Transactional(readOnly = true)
    public List<ContratoResponse> meusContratos(Integer viewerId, String papel) {
        List<Contrato> base = new ArrayList<>();
        if (papel == null || papel.equalsIgnoreCase("cliente")) {
            base.addAll(contratos.findByCliente_IdOrderByCriadoEmDesc(viewerId));
        }
        if (papel == null || papel.equalsIgnoreCase("profissional")) {
            base.addAll(contratos.findByProfissional_IdOrderByCriadoEmDesc(viewerId));
        }
        base.sort(Comparator.comparing(Contrato::getCriadoEm).reversed());
        return base.stream().map(this::mapear).toList();
    }

    /** Um contrato (partes ou staff; 404 para não revelar existência). */
    @Transactional(readOnly = true)
    public ContratoResponse buscarPorId(Integer viewerId, Integer id) {
        return mapear(exigirAcesso(viewerId, id));
    }

    /** Edita dados (cliente/staff, só não-finalizado, null = não mexer). */
    @Transactional
    public ContratoResponse atualizar(Integer viewerId, Integer id, AtualizarContratoRequest req, String ip) {
        Contrato contrato = exigirAcesso(viewerId, id);
        if (!contrato.getCliente().getId().equals(viewerId) && !usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente o cliente ou a equipe Elora pode editar o contrato");
        }
        if (FINAIS.contains(contrato.getStatus())) {
            throw new BusinessException("Contrato finalizado não pode ser editado");
        }
        if (req.getTitulo() != null) {
            contrato.setTitulo(req.getTitulo().trim());
        }
        if (req.getDescricaoNecessidade() != null) {
            contrato.setDescricaoNecessidade(vazioParaNull(req.getDescricaoNecessidade()));
        }
        if (req.getValorHora() != null) {
            contrato.setValorHora(req.getValorHora());
        }
        if (req.getValorTotal() != null) {
            contrato.setValorTotal(req.getValorTotal());
        }
        if (req.getEnderecoAtendimento() != null) {
            contrato.setEnderecoAtendimento(vazioParaNull(req.getEnderecoAtendimento()));
        }
        if (req.getDataInicio() != null) {
            contrato.setDataInicio(req.getDataInicio());
        }
        if (req.getDataFim() != null) {
            contrato.setDataFim(req.getDataFim());
        }
        validarDatas(contrato.getDataInicio(), contrato.getDataFim());
        Contrato salvo = contratos.save(contrato);
        auditoria.registrar(viewerId, "contrato.atualizar", "contrato", salvo.getId(), ip);
        return mapear(salvo);
    }

    /** Avança no funil (transição proibida = 422). */
    @Transactional
    public ContratoResponse atualizarStatus(Integer viewerId, Integer id, StatusContrato novoStatus, String ip) {
        Contrato contrato = exigirAcesso(viewerId, id);
        Set<StatusContrato> permitidos = TRANSICOES.get(contrato.getStatus());
        if (permitidos == null || !permitidos.contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de " + contrato.getStatus() + " para " + novoStatus + " não é permitida");
        }
        contrato.setStatus(novoStatus);
        Contrato salvo = contratos.save(contrato);
        auditoria.registrar(viewerId, "contrato.status", "contrato", salvo.getId(), ip);
        Integer outro = salvo.getCliente().getId().equals(viewerId)
                ? salvo.getProfissional().getId() : salvo.getCliente().getId();
        notificacoes.notificarSistema(outro, "Contrato " + novoStatus,
                "Contrato " + salvo.getCodigo() + " agora está " + novoStatus, "contrato", salvo.getId());
        return mapear(salvo);
    }

    /**
     * Assinatura digital simplificada (hash SHA-256 do termo, provedor interno).
     * Só as partes assinam, só em aguard_assinatura; com as duas assinaturas o
     * contrato ativa sozinho e libera o pagamento.
     */
    @Transactional
    public ContratoResponse assinar(Integer viewerId, Integer id, String ip) {
        Contrato contrato = exigirAcesso(viewerId, id);
        if (contrato.getStatus() != StatusContrato.aguard_assinatura) {
            throw new BusinessException("Contrato não está aguardando assinatura");
        }
        String papel;
        if (viewerId.equals(contrato.getCliente().getId())) {
            papel = "cliente";
        } else if (viewerId.equals(contrato.getProfissional().getId())) {
            papel = "profissional";
        } else {
            throw new ForbiddenException("Somente as partes assinam o contrato");
        }
        if (assinaturas.existsByContratoIdAndUsuario_IdAndPapel(id, viewerId, papel)) {
            throw new BusinessException("Você já assinou este contrato");
        }
        AssinaturaContrato a = new AssinaturaContrato();
        a.setContratoId(id);
        a.setUsuario(usuarioService.getVisivel(viewerId));
        a.setPapel(papel);
        a.setHashDocumento(sha256Hex(contrato.getCodigo() + "|" + viewerId + "|" + papel
                + "|" + System.currentTimeMillis()));
        a.setProvedor("interno");
        a.setIpAssinatura(ip);
        assinaturas.save(a);
        auditoria.registrar(viewerId, "contrato.assinar", "contrato", id, ip);

        boolean clienteAssinou = assinaturas.existsByContratoIdAndUsuario_IdAndPapel(
                id, contrato.getCliente().getId(), "cliente");
        boolean profissionalAssinou = assinaturas.existsByContratoIdAndUsuario_IdAndPapel(
                id, contrato.getProfissional().getId(), "profissional");
        if (clienteAssinou && profissionalAssinou) {
            contrato.setStatus(StatusContrato.ativo);
            contratos.save(contrato);
            auditoria.registrar(viewerId, "contrato.ativar", "contrato", id, ip);
            notificacoes.notificarSistema(contrato.getCliente().getId(), "Contrato ativo",
                    "Contrato " + contrato.getCodigo() + " assinado pelas partes. Pagamento liberado.",
                    "contrato", id);
            notificacoes.notificarSistema(contrato.getProfissional().getId(), "Contrato ativo",
                    "Contrato " + contrato.getCodigo() + " assinado pelas partes. Bom trabalho!",
                    "contrato", id);
        } else {
            Integer outro = papel.equals("cliente")
                    ? contrato.getProfissional().getId() : contrato.getCliente().getId();
            notificacoes.notificarSistema(outro, "Assinatura pendente",
                    "A outra parte assinou o contrato " + contrato.getCodigo() + ". Falta você!",
                    "contrato", id);
        }
        return mapear(contratos.findById(id).orElseThrow());
    }

    /** Exclui fisicamente SÓ rascunho (criador/cliente/staff). */
    @Transactional
    public void excluir(Integer viewerId, Integer id, String ip) {
        Contrato contrato = exigirAcesso(viewerId, id);
        if (contrato.getStatus() != StatusContrato.rascunho) {
            throw new BusinessException("Somente contratos em rascunho podem ser excluídos");
        }
        boolean criador = contrato.getCriadoPor() != null && contrato.getCriadoPor().getId().equals(viewerId);
        boolean cliente = contrato.getCliente().getId().equals(viewerId);
        if (!criador && !cliente && !usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente o criador ou a equipe Elora pode excluir este rascunho");
        }
        auditoria.registrar(viewerId, "contrato.excluir", "contrato", id, ip);
        contratos.delete(contrato);
    }

    private Contrato exigirAcesso(Integer viewerId, Integer id) {
        var comoCliente = contratos.findByIdAndCliente_Id(id, viewerId);
        if (comoCliente.isPresent()) {
            return comoCliente.get();
        }
        var comoProfissional = contratos.findByIdAndProfissional_Id(id, viewerId);
        if (comoProfissional.isPresent()) {
            return comoProfissional.get();
        }
        if (usuarioService.ehStaff(viewerId)) {
            return contratos.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        }
        throw new ResourceNotFoundException("Contrato não encontrado");
    }

    private void validarDatas(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("dataFim não pode ser anterior à dataInicio");
        }
    }

    private String gerarCodigoUnico() {
        int ano = LocalDate.now().getYear();
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            String codigo = String.format("ELO-%d-%06d", ano,
                    ThreadLocalRandom.current().nextInt(0, 1000000));
            if (!contratos.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new BusinessException("Não foi possível gerar um código único, tente novamente");
    }

    private String vazioParaNull(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return texto.trim();
    }

    private String sha256Hex(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private ContratoResponse mapear(Contrato c) {
        List<AssinaturaResponse> sigs = assinaturas.findByContratoId(c.getId()).stream()
                .map(a -> new AssinaturaResponse(a.getId(), a.getPapel(), a.getAssinadoEm()))
                .toList();
        return new ContratoResponse(
                c.getId(), c.getCodigo(), c.getCliente().getId(), c.getProfissional().getId(),
                c.getTitulo(), c.getDescricaoNecessidade(), c.getValorHora(), c.getValorTotal(),
                c.getEnderecoAtendimento(), c.getStatus(), c.getDataInicio(), c.getDataFim(),
                sigs, c.getCriadoEm(), c.getAtualizadoEm());
    }
}
