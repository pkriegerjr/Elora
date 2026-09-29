package com.elora.module.profissional.service;
import com.elora.common.exception.*; import com.elora.module.profissional.dto.*;
import com.elora.module.profissional.entity.*; import com.elora.module.profissional.enums.Periodo; import com.elora.module.profissional.mapper.ProfissionalMapper;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.profissional.enums.Periodo;
import com.elora.module.profissional.repository.*; import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.usuario.enums.StatusVerificacao; import com.elora.module.usuario.repository.*;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*; 

@Service @RequiredArgsConstructor
public class ProfissionalService {
  private final UsuarioRepository usuarios;
  private final ProfissionalDetalhesRepository detalhesRepo;
  private final DocumentoProfissionalRepository docsRepo;
  private final ProfissionalDisponibilidadeRepository dispRepo;
  private final ProfissionalEspecialidadeRepository espRepo;
  private final ProfissionalUsuarioEspecialidadeRepository ueRepo;
  private final UsuarioPerfilRepository usuarioPerfis;
  private final ProfissionalMapper mapper;

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
    dto.setStatus(d.getStatus().name()); return dto;
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
        String nomeNormalizado = nome.trim();
        var esp = espRepo.findByNomeIgnoreCase(nomeNormalizado)
          .orElseGet(() -> {
            var nova = new Especialidade();
            nova.setNome(nomeNormalizado);
            return espRepo.save(nova);
          });
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

public DocumentoProfissional uploadDocumento(Integer profId, String tipo, String url){
  getProfissional(profId);
  if(tipo==null||tipo.isBlank()||url==null||url.isBlank()) throw new BusinessException("tipo e arquivoUrl obrigatórios");
  var doc=new DocumentoProfissional(); doc.setUsuarioId(profId);
  doc.setTipo(tipo); doc.setArquivoUrl(url);
  doc.setStatus(com.elora.module.profissional.enums.DocumentoStatus.pendente);
  return docsRepo.save(doc);
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
    return getById(profId);
  }

  @Transactional public void salvarDisponibilidade(Integer profId, List<Map<String,String>> body){
  dispRepo.deleteByUsuarioId(profId);
  for(var p: body){
    var disp=new Disponibilidade(); disp.setUsuarioId(profId);
    java.time.LocalDate data;
    try{data=java.time.LocalDate.parse(p.get("data"));}catch(Exception e){throw new BusinessException("data inválida, use yyyy-MM-dd");}
    if(data.isBefore(java.time.LocalDate.now())) throw new BusinessException("data no passado");
    disp.setData(data);
    try{disp.setPeriodo(Periodo.valueOf(p.get("periodo").toLowerCase().trim()));}catch(Exception e){throw new BusinessException("periodo use: matutino|vespertino|noturno");}
    dispRepo.save(disp);
  }
}
  @Transactional(readOnly=true) public List<Disponibilidade> getDisponibilidade(Integer profId){ return dispRepo.findByUsuarioId(profId); }
}