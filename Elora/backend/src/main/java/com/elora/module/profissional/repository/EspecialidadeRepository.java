package com.elora.module.profissional.repository;
import com.elora.module.profissional.entity.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
import org.springframework.stereotype.Repository;
@Repository("profissionalEspecialidadeRepository")
public interface EspecialidadeRepository extends JpaRepository<Especialidade,Integer> {
  Optional<Especialidade> findByNome(String nome);
  Optional<Especialidade> findByNomeIgnoreCase(String nome);
}