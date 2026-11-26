package com.elora.module.profissional.service;
import com.elora.common.exception.*; import com.elora.integration.storage.*;
import com.elora.module.profissional.dto.*;
import com.elora.module.profissional.entity.*; import com.elora.module.profissional.enums.DocumentoStatus; import com.elora.module.profissional.enums.Periodo; import com.elora.module.profissional.mapper.ProfissionalMapper;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.notificacao.service.AvisoSistemaService;
import com.elora.module.profissional.repository.*; import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.usuario.enums.StatusVerificacao; import com.elora.module.usuario.repository.*;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.time.LocalDate; import java.util.*;

@Service @RequiredArgsConstructor
public class ProfissionalService {
  /** Formatos aceitos no upload de documento. */
  private static final Set<String> EXTENSOES_DOCUMENTO = Set.of("pdf", "jpg", "jpeg", "png");

  private final UsuarioRepository usuarios;
  private final ProfissionalDetalhesRepository detalhesRepo;
  private final DocumentoProfissionalRepository docsRepo;
  private final DisponibilidadeRepository dispRepo;
  private final EspecialidadeRepository espRepo;
  private final UsuarioEspecialidadeRepository ueRepo;
  private final UsuarioPerfilRepository usuarioPerfis;
  private final ProfissionalMapper mapper;
  private final StorageService storage;
  private final UsuarioService usuariosService;
  private final AvisoSistemaService avisos;

  /** Limite da aplicação (menor que o do multipart): erro 422 com mensagem clara. */
  @Value("${app.integracoes.storage.max-bytes:5242880}")
  private long maxBytes = 5_242_880;

  private Usuario getProfissional(Integer id){ // valida que é profissional
    var u = usuarios.findById(id).filter(x->x.getDeletedAt()==null)
      .orElseThrow(()->new ResourceNotFoundException("Profissional não encontrado"));
    if(usuarioPerfis.findByUsuario_Id(id).stream().noneMatch(up->up.getPerfil().getNome().equals("profissional")))
      throw new BusinessException("Usuário não é profissional");
    return u;
  }

  @Transactional(readOnly=true)
  public ProfissionalResponse getById(Integer id){
    var u=getProfissional(id);
    var d=detalhesRepo.findByUsuarioId(id).orElse(null);
    List<String> specs = ueRepo.findNomesByUsuarioId(id); // FIX: antes era limit(0)
    var docs = docsRepo.findByUsuarioId(id).stream().map(this::toDocDTO).toList();
    return mapper.toResponse(u,d,specs,docs);
  }
  private ProfissionalResponse.DocumentoDTO toDocDTO(DocumentoProfissional d){
    var dto=new ProfissionalResponse.DocumentoDTO();
    dto.setId(d.getId()); dto.setTipo(d.getTipo()); dto.setArquivoUrl(d.getArquivoUrl());
    dto.setStatus(d.getStatus().name());
    dto.setUrlDownload("/caregivers/"+d.getUsuarioId()+"/documentos/"+d.getId()+"/arquivo");
    return dto;
  }

  @Transactional
  public ProfissionalResponse updateMe(Integer authId, ProfissionalUpdateRequest req){
    var u=getProfissional(authId);
    if(req.getNome()!=null) u.setNome(req.getNome());
    if(req.getTelefone()!=null) u.setTelefone(req.getTelefone());
    usuarios.save(u);
    var d=detalhesRepo.findByUsuarioId(authId).orElseThrow(()->new ResourceNotFoundException("Detalhes não encontrado"));
    if(req.getDescricaoPerfil()!=null) d.setDescricaoPerfil(req.getDescricaoPerfil());
    if(req.getPrecoHora()!=null) d.setPrecoHora(req.getPrecoHora());
    detalhesRepo.save(d);
    if(req.getEspecialidades()!=null){
      ueRepo.deleteByUsuarioId(authId);
      for(String nome: req.getEspecialidades()){
        var esp = espRepo.findByNomeIgnoreCase(nome)
          .orElseGet(()-> espRepo.save(new Especialidade(null, nome.trim())));
        ueRepo.save(new UsuarioEspecialidade(authId, esp.getId(), null));
      }
    }
    return getById(authId);
  }

  @Transactional(readOnly=true)
  public List<ProfissionalResponse> list(String status, int page, int size){
    return detalhesRepo.findAll().stream()
      .filter(d-> status==null || d.getStatusVerificacao().name().equalsIgnoreCase(status))
      .skip((long)page*size).limit(size)
      .map(d-> getById(d.getUsuarioId())).toList();
  }

  /** Legado (JSON com URL pronta). Mantido para compatibilidade, agora com checagem de dono. */
  @Transactional
  public DocumentoProfissional uploadDocumento(Integer profId, Integer authId, String tipo, String url){
    getProfissional(profId);
    exigirDonoOuStaff(profId, authId, "anexar documento a este profissional");
    if(tipo==null||tipo.isBlank()||url==null||url.isBlank()) throw new BusinessException("tipo e arquivoUrl obrigatórios");
    var doc=new DocumentoProfissional(); doc.setUsuarioId(profId);
    doc.setTipo(tipo); doc.setArquivoUrl(url);
    doc.setStatus(DocumentoStatus.pendente);
    return docsRepo.save(doc);
  }

  /** Upload real: valida permissão, limite, formato; grava no storage e só então no banco. */
  @Transactional
  public ProfissionalResponse.DocumentoDTO anexarDocumento(Integer profId, Integer authId, String tipo, MultipartFile arquivo){
    getProfissional(profId);
    exigirDonoOuStaff(profId, authId, "anexar documento a este profissional");
    if(tipo==null||tipo.isBlank()) throw new BusinessException("tipo é obrigatório (ex.: RG, CPF, certificado_curso)");
    if(arquivo==null||arquivo.isEmpty()) throw new BusinessException("arquivo é obrigatório");
    if(arquivo.getSize()>maxBytes)
      throw new BusinessException("Arquivo maior que o limite de "+(maxBytes/1024/1024)+" MB");
    String nome = arquivo.getOriginalFilename()==null ? "documento" : arquivo.getOriginalFilename();
    String ext = extensao(nome);
    if(!EXTENSOES_DOCUMENTO.contains(ext))
      throw new BusinessException("Formato inválido: envie PDF, JPG ou PNG");
    String contentType = switch(ext){
      case "pdf" -> "application/pdf";
      case "png" -> "image/png";
      default -> "image/jpeg";
    };

    ArquivoArmazenado salvo;
    try(var in = arquivo.getInputStream()){
      salvo = storage.salvar(in, arquivo.getSize(), nome, contentType);
    }catch(IOException e){
      throw new BusinessException("Falha ao ler o arquivo enviado");
    }
    try{
      var doc=new DocumentoProfissional();
      doc.setUsuarioId(profId); doc.setTipo(tipo.trim());
      doc.setArquivoUrl(salvo.chave()); doc.setStatus(DocumentoStatus.pendente);
      return toDocDTO(docsRepo.save(doc));
    }catch(RuntimeException e){
      storage.remover(salvo.chave()); // não deixa arquivo órfão se o banco recusar
      throw e;
    }
  }

  /** Download: só o dono do documento ou staff. */
  @Transactional(readOnly=true)
  public ArquivoLido baixarDocumento(Integer profId, Integer docId, Integer authId){
    getProfissional(profId);
    exigirDonoOuStaff(profId, authId, "baixar este documento");
    var doc = docsRepo.findById(docId).filter(d->profId.equals(d.getUsuarioId()))
      .orElseThrow(()->new ResourceNotFoundException("Documento não encontrado"));
    return storage.ler(doc.getArquivoUrl());
  }

  private void exigirDonoOuStaff(Integer profId, Integer authId, String acao){
    if(!profId.equals(authId) && !usuariosService.ehStaff(authId))
      throw new ForbiddenException("Sem permissão para "+acao);
  }

  private static String extensao(String nome){
    if(nome==null) return "";
    int i=nome.lastIndexOf('.');
    return i<0 || i==nome.length()-1 ? "" : nome.substring(i+1).toLowerCase(Locale.ROOT);
  }

  @Transactional
  public ProfissionalResponse validar(Integer profId, Integer validadorId, ValidacaoRequest req){
    var d=detalhesRepo.findByUsuarioId(profId).orElseThrow(()->new ResourceNotFoundException("Profissional não encontrado"));
    // regra integrada com UsuarioService.java:40 PERFIS_STAFF
    var novo = switch(req.getResultado().toLowerCase()){
      case "aprovado" -> StatusVerificacao.aprovado;
      case "reprovado","rejeitado" -> StatusVerificacao.rejeitado;
      case "correcao" -> StatusVerificacao.correcao;
      default -> throw new BusinessException("Resultado inválido use: aprovado|reprovado|correcao");
    };
    if(d.getStatusVerificacao()==StatusVerificacao.aprovado && novo!=StatusVerificacao.aprovado)
      throw new BusinessException("Profissional já aprovado");
    d.setStatusVerificacao(novo);
    d.setDocumentoVerificado(novo==StatusVerificacao.aprovado);
    detalhesRepo.save(d);

    String titulo = switch(novo){
      case aprovado -> "Cadastro aprovado";
      case rejeitado -> "Cadastro rejeitado";
      case correcao -> "Correção solicitada";
      default -> "Cadastro atualizado";
    };
    String descricao = switch(novo){
      case aprovado -> "aprovado";
      case rejeitado -> "rejeitado";
      case correcao -> "com correção solicitada";
      default -> "atualizado";
    };
    String observacao = req.getObservacao()==null || req.getObservacao().isBlank()
      ? "" : " Observação: " + req.getObservacao().trim();
    avisos.notificarSeguroUsuario(profId, titulo,
      "Seu cadastro foi " + descricao + "." + observacao + " Acesse seu painel para detalhes.",
      "validacao", profId);
    return getById(profId);
  }

  /**
   * Substitui a disponibilidade do profissional. O delete precisa ser enviado ao
   * banco antes dos inserts: a entidade usa IDENTITY, então cada save() executa
   * um INSERT imediato. Sem o flush, o UNIQUE (usuario_id, data, periodo) barra
   * o próprio registro que ainda está sendo removido.
   */
  @Transactional public void salvarDisponibilidade(Integer profId, List<Map<String,String>> body){
    LocalDate hoje = LocalDate.now();
    Map<LocalDate, Set<Periodo>> porData = new LinkedHashMap<>();

    for(var p: body == null ? List.<Map<String,String>>of() : body){
      if(p == null) continue;
      LocalDate data;
      try{ data = LocalDate.parse(p.get("data")); }catch(Exception e){ throw new BusinessException("data inválida, use yyyy-MM-dd"); }
      if(data.isBefore(hoje)) throw new BusinessException("data no passado");

      Periodo periodo;
      try{ periodo = Periodo.valueOf(p.get("periodo").toLowerCase().trim()); }
      catch(Exception e){ throw new BusinessException("periodo use: matutino|vespertino|noturno"); }
      porData.computeIfAbsent(data, k -> new LinkedHashSet<>()).add(periodo);
    }

    dispRepo.deleteByUsuarioId(profId);
    dispRepo.flush();

    for(var entry : porData.entrySet()){
      for(Periodo periodo : entry.getValue()){
        var disp = new Disponibilidade();
        disp.setUsuarioId(profId);
        disp.setData(entry.getKey());
        disp.setPeriodo(periodo);
        disp.setStatus("disponivel");
        dispRepo.save(disp);
      }
    }
  }
  @Transactional(readOnly=true) public List<Disponibilidade> getDisponibilidade(Integer profId){ return dispRepo.findByUsuarioId(profId); }
}
