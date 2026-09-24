package com.elora.module.profissional.repository;
import com.elora.module.profissional.entity.DocumentoProfissional;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface DocumentoProfissionalRepository extends JpaRepository<DocumentoProfissional,Integer> {
  List<DocumentoProfissional> findByUsuarioId(Integer usuarioId);
}