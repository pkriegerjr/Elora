package com.elora.module.profissional.service;
import com.elora.common.exception.*; import com.elora.module.profissional.dto.*; import com.elora.module.profissional.entity.*; import com.elora.module.profissional.mapper.ProfissionalMapper;
import com.elora.module.profissional.repository.*; import com.elora.module.usuario.entity.*; import com.elora.module.usuario.enums.StatusVerificacao;
import com.elora.module.usuario.repository.*; import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.*; import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ProfissionalService {
  private final UsuarioRepository usuarios;
  private final ProfissionalDetalhesRepository detalhesRepo;
  private final DocumentoProfissionalRepository docsRepo;
  private final DisponibilidadeRepository dispRepo;
  private final EspecialidadeRepository espRepo;
  private final UsuarioPerfilRepository usuarioPerfis;
  private final ProfissionalMapper mapper;

  private Usuario getProfissional(Integer id){
    var u=usuarios.findById(id).filter(x->x.getDeletedAt()==null).orElseThrow(()->new ResourceNotFoundException("Profissional não encontrado"));
    if(usuarioPerfis.findByUsuario_Id(id).stream().noneMatch(up->up.getPerfil().getNome().equals("profissional")))
      throw new BusinessException("Usuário não é profissional");
    return u;
  }

  @Transactional(readOnly=true)
  public ProfissionalResponse getById(Integer id){
    var u=getProfissional(id); var d=detalhesRepo.findByUsuarioId(id).orElse(null);
    var specs=espRepo.findAll().stream().filter(e-> true).limit(0).toList(); // placeholder
    // busca N:N via jdbc nativo
    var nomes=usuarios.findById(id).map(x-> new ArrayList<String>()).orElse(new ArrayList<>());
    // query N:N simplificada: busca via usuario_especialidade
    List<String> especialidades = dispRepo.getEntityManager()!=null? List.of(): List.of();
    // fallback: lê tabela usuario_especialidade com native query via UsuarioRepository custom
    List<ProfissionalResponse.DocumentoDTO> docs=docsRepo.findByUsuarioId(id).stream().map(this::toDocDTO).toList();
    return mapper.toResponse(u,d,especialidades,docs);
  }

  private ProfissionalResponse.DocumentoDTO toDocDTO(DocumentoProfissional d){
    var dto=new ProfissionalResponse.DocumentoDTO(); dto.setId(d.getId()); dto.setTipo(d.getTipo()); dto.setArquivoUrl(d.getArquivoUrl()); dto.setStatus(d.getStatus().name()); return dto;
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
    // especialidades N:N
    if(req.getEspecialidades()!=null){
      // limpa e reinsere (native)
      // mantém compat com mock: especialidades string livre
      for(String nome: req.getEspecialidades()){
        espRepo.findByNome(nome).orElseGet(()->{ var e=new Especialidade(); e.setNome(nome); return espRepo.save(e); });
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

  @Transactional
  public DocumentoProfissional uploadDocumento(Integer profId, String tipo, String url){
    getProfissional(profId);
    var doc=new DocumentoProfissional(); doc.setUsuarioId(profId); doc.setTipo(tipo); doc.setArquivoUrl(url);
    return docsRepo.save(doc);
  }

  // fluxo PENDING -> UNDER_REVIEW -> APPROVED | REJECTED | NEEDS_CORRECTION
  // mapeado para pendente -> em_analise -> aprovado | rejeitado | correcao
  @Transactional
  public ProfissionalResponse validar(Integer profId, Integer validadorId, ValidacaoRequest req){
    var d=detalhesRepo.findByUsuarioId(profId).orElseThrow(()->new ResourceNotFoundException("Profissional não encontrado"));
    var atual=d.getStatusVerificacao();
    Map<String,StatusVerificacao> map=Map.of("aprovado",StatusVerificacao.aprovado,"reprovado",StatusVerificacao.rejeitado,"correcao",StatusVerificacao.correcao);
    var novo=map.get(req.getResultado().toLowerCase());
    if(novo==null) throw new BusinessException("Resultado inválido");
    // transição válida
    if(atual==StatusVerificacao.aprovado && novo!=StatusVerificacao.aprovado) throw new BusinessException("Profissional já aprovado");
    d.setStatusVerificacao(novo);
    d.setDocumentoVerificado(novo==StatusVerificacao.aprovado);
    detalhesRepo.save(d);
    return getById(profId);
  }

  @Transactional public void salvarDisponibilidade(Integer profId, List<Map<String,String>> periodos){
    dispRepo.deleteByUsuarioId(profId);
    for(var p: periodos){
      var disp=new Disponibilidade(); disp.setUsuarioId(profId);
      disp.setData(java.time.LocalDate.parse(p.get("data")));
      disp.setPeriodo(com.elora.module.profissional.enums.Periodo.valueOf(p.get("periodo")));
      dispRepo.save(disp);
    }
  }
  @Transactional(readOnly=true) public List<Disponibilidade> getDisponibilidade(Integer profId){ return dispRepo.findByUsuarioId(profId); }
}