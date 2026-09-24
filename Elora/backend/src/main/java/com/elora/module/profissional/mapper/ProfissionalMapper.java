package com.elora.module.profissional.mapper;
import com.elora.module.usuario.entity.Usuario; import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.profissional.dto.ProfissionalResponse;
import org.springframework.stereotype.Component; import java.util.*;
@Component
public class ProfissionalMapper {
  public ProfissionalResponse toResponse(Usuario u, ProfissionalDetalhes d, List<String> specs, List<ProfissionalResponse.DocumentoDTO> docs){
    var r=new ProfissionalResponse();
    r.setId(u.getId()); r.setNome(u.getNome()); r.setEmail(u.getEmail()); r.setCpf(u.getCpf());
    r.setTelefone(u.getTelefone()); r.setFotoUrl(u.getFotoUrl()); r.setGenero(u.getGenero()!=null?u.getGenero().name():null);
    if(d!=null){ r.setPrecoHora(d.getPrecoHora()); r.setDescricaoPerfil(d.getDescricaoPerfil());
      r.setStatusVerificacao(d.getStatusVerificacao().name()); r.setDocumentoVerificado(d.getDocumentoVerificado()); r.setNotaMedia(d.getNotaMedia()); }
    r.setEspecialidades(specs); r.setDocumentos(docs); return r;
  }
}