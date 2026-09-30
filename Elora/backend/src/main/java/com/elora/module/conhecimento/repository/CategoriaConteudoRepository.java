package com.elora.module.conhecimento.repository;
import com.elora.module.conhecimento.entity.CategoriaConteudo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CategoriaConteudoRepository extends JpaRepository<CategoriaConteudo, Integer> {
    Optional<CategoriaConteudo> findByNome(String nome);
}