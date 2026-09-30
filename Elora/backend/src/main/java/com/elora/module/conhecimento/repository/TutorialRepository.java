package com.elora.module.conhecimento.repository;
import com.elora.module.conhecimento.entity.Tutorial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TutorialRepository extends JpaRepository<Tutorial, Integer> {
    List<Tutorial> findByStatus(String status);
}