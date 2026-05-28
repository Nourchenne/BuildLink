package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Long> {

    // Demandes d'un architecte
    List<MaterialRequest> findByArchitect(User architect);

    // Toutes les demandes ouvertes (pour les fournisseurs)
    List<MaterialRequest> findByStatus(RequestStatus status);

    // Demandes d'un projet
    List<MaterialRequest> findByProject(Project project);
}