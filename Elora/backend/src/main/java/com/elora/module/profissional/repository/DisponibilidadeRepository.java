package com.elora.module.profissional.repository;
import com.elora.module.profissional.entity.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository("profissionalDisponibilidadeRepository")
public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade,Integer> {
    List<Disponibilidade> findByUsuarioId(Integer id);
    void deleteByUsuarioId(Integer id); // usado em salvarDisponibilidade
}