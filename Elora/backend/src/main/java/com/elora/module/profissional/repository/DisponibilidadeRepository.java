package com.elora.module.profissional.repository;
import com.elora.module.profissional.entity.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade,Integer> {
  List<Disponibilidade> findByUsuarioId(Integer id);
  void deleteByUsuarioId(Integer id);
}