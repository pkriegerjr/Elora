package com.elora.module.conhecimento.repository;
import com.elora.module.conhecimento.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FaqRepository extends JpaRepository<Faq, Integer> {
    List<Faq> findByStatus(String status);
}