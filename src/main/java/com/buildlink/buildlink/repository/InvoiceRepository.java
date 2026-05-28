package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.Invoice;
import com.buildlink.buildlink.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByProject(Project project);
    boolean existsByProject(Project project);
}