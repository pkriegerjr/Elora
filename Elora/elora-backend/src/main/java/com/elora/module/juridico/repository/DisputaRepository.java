package com.elora.module.juridico.repository;

import com.elora.module.juridico.entity.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DisputaRepository extends JpaRepository<Disputa, Long> { }
