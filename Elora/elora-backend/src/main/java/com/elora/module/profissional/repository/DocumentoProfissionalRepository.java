package com.elora.module.profissional.repository;

import com.elora.module.profissional.entity.DocumentoProfissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoProfissionalRepository extends JpaRepository<DocumentoProfissional, Long> { }
