package com.elora.module.financeiro.repository;

import com.elora.module.financeiro.entity.Repasse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepasseRepository extends JpaRepository<Repasse, Long> { }
