package com.elora.module.contrato.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.dto.AtualizarContratoRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.mapper.ContratoMapper;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MÓDULO CONTRATO - Regras de negócio (ciclo de vida vindo da documentação).
 *
 * <p>Funil (schema CHECK + fluxo do front): rascunho → proposta → negociacao →
 * aguard_assinatura → ativo → concluido; desvios: cancelado (antes de ativo),
 * rescindido e em_disputa (a partir de ativo/disputa). Finais não saem do lugar.
 * Cria: cliente dono ou staff; vê: partes ou staff; edita: cliente/staff e só
 * não-finalizado; exclui fisicamente: só rascunho (histórico se preserva).</p>
 */
@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratos;
    private final UsuarioService usuarioService;

    private static final String PERFIL_PROFISSIONAL = "profissional";

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

    /** Cria (sempre rascunho, código único gerado na app). */
    @Transactional
    public ContratoResponse criar(Integer viewerId, CriarContratoRequest req) {
        Usuario cliente = usuarioService.getVisivel(req.getClienteId());
        Usuario profissional = usuarioService.getVisivel(req.getProfissionalId());

        if (!cliente.getId().equals(viewerId) && !usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente o próprio cliente ou a equipe Elora pode criar o contrato");
        }
        if (cliente.getId().equals(profissional.getId())) {
            throw new BusinessException("Cliente e profissional não podem ser o mesmo usuário");
        }
        if (!usuarioService.perfisDe(profissional.getId()).contains(PERFIL_PROFISSIONAL)) {
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
        return ContratoMapper.toResponse(contratos.save(contrato));
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
        return base.stream().map(ContratoMapper::toResponse).toList();
    }

    /** Um contrato (partes ou staff; 404 para não revelar existência). */
    @Transactional(readOnly = true)
    public ContratoResponse buscarPorId(Integer viewerId, Integer id) {
        return ContratoMapper.toResponse(exigirAcesso(viewerId, id));
    }

    /** Edita dados (cliente/staff, só não-finalizado, null = não mexer). */
    @Transactional
    public ContratoResponse atualizar(Integer viewerId, Integer id, AtualizarContratoRequest req) {
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
        return ContratoMapper.toResponse(contratos.save(contrato));
    }

    /** Avança no funil (transição proibida = 422). */
    @Transactional
    public ContratoResponse atualizarStatus(Integer viewerId, Integer id, StatusContrato novoStatus) {
        Contrato contrato = exigirAcesso(viewerId, id);
        Set<StatusContrato> permitidos = TRANSICOES.get(contrato.getStatus());
        if (permitidos == null || !permitidos.contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de " + contrato.getStatus() + " para " + novoStatus + " não é permitida");
        }
        contrato.setStatus(novoStatus);
        return ContratoMapper.toResponse(contratos.save(contrato));
    }

    /** Exclui fisicamente SÓ rascunho (criador/cliente/staff). */
    @Transactional
    public void excluir(Integer viewerId, Integer id) {
        Contrato contrato = exigirAcesso(viewerId, id);
        if (contrato.getStatus() != StatusContrato.rascunho) {
            throw new BusinessException("Somente contratos em rascunho podem ser excluídos");
        }
        boolean criador = contrato.getCriadoPor() != null && contrato.getCriadoPor().getId().equals(viewerId);
        boolean cliente = contrato.getCliente().getId().equals(viewerId);
        if (!criador && !cliente && !usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente o criador ou a equipe Elora pode excluir este rascunho");
        }
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
            String codigo = String.format("ELO-%d-%06d", ano, ThreadLocalRandom.current().nextInt(0, 1000000));
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
}
