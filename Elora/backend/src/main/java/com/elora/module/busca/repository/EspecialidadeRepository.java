package com.elora.module.busca.repository;
import com.elora.module.busca.entity.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
/** MÓDULO BUSCA - Catálogo de tags (ordenado p/ o filtro do front). */
@Repository("buscaEspecialidadeRepository")
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Integer> {
    List<Especialidade> findAllByOrderByNomeAsc();
}