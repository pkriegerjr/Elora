package com.elora.module.busca.repository;

import com.elora.module.busca.entity.BuscaProfissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuscaProfissionalRepository extends JpaRepository<BuscaProfissional, Long> { }
