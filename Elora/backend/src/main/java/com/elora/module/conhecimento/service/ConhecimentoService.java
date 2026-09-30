package com.elora.module.conhecimento.service;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.conhecimento.dto.*;
import com.elora.module.conhecimento.entity.*;
import com.elora.module.conhecimento.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor
public class ConhecimentoService {
    private final ArtigoConhecimentoRepository artigos;
    private final CategoriaConteudoRepository categorias;
    private final FaqRepository faqs;
    private final TutorialRepository tutoriais;

    @Transactional
    public ArtigoResponse criarArtigo(ArtigoRequest req, Integer autorId) {
        var cat = req.getCategoriaId() != null ? categorias.findById(req.getCategoriaId()).orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada")) : null;
        var a = new ArtigoConhecimento();
        a.setTitulo(req.getTitulo()); a.setCorpo(req.getCorpo());
        a.setCategoria(req.getCategoria()); a.setCategoriaRef(cat);
        a.setAutorId(autorId); a.setPublicado(req.getPublicado() != null ? req.getPublicado() : false);
        a = artigos.save(a);
        return toResponse(a);
    }
    @Transactional(readOnly = true)
    public List<ArtigoResponse> listarPublicados() { return artigos.findByPublicadoTrue().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public ArtigoResponse buscarArtigo(Integer id) {
        var a = artigos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado"));
        return toResponse(a);
    }
    private ArtigoResponse toResponse(ArtigoConhecimento a) {
        return ArtigoResponse.builder().id(a.getId()).titulo(a.getTitulo()).corpo(a.getCorpo())
            .categoria(a.getCategoria()).categoriaId(a.getCategoriaRef() != null ? a.getCategoriaRef().getId() : null)
            .autorId(a.getAutorId()).publicado(a.getPublicado()).criadoEm(a.getCriadoEm()).build();
    }
    @Transactional(readOnly = true)
    public List<FaqResponseDTO> listarFaqPublicado() {
        return faqs.findByStatus("publicado").stream().map(f -> FaqResponseDTO.builder()
            .id(f.getId()).pergunta(f.getPergunta()).resposta(f.getResposta())
            .categoria(f.getCategoria()).status(f.getStatus())
            .categoriaId(f.getCategoriaRef() != null ? f.getCategoriaRef().getId() : null)
            .categoriaNome(f.getCategoriaRef() != null ? f.getCategoriaRef().getNome() : null)
            .criadoEm(f.getCriadoEm()).build()).toList();
    }
}