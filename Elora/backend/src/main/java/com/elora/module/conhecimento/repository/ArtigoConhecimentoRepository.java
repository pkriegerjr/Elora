package com.elora.module.conhecimento.repository;
import com.elora.module.conhecimento.entity.ArtigoConhecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ArtigoConhecimentoRepository extends JpaRepository<ArtigoConhecimento, Integer> {
    List<ArtigoConhecimento> findByPublicadoTrue();
}