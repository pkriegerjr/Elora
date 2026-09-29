package com.elora.module.profissional.repository;
import com.elora.module.profissional.entity.UsuarioEspecialidade;
import com.elora.module.profissional.entity.UsuarioEspecialidadeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface ProfissionalUsuarioEspecialidadeRepository
        extends JpaRepository<UsuarioEspecialidade, Integer> {
    void deleteByUsuarioId(Integer usuarioId);
    @Query("SELECT e.nome FROM ProfissionalUsuarioEspecialidade ue JOIN ue.especialidade e WHERE ue.usuarioId = :uid ORDER BY e.nome")
    List<String> findNomesByUsuarioId(Integer uid);
}