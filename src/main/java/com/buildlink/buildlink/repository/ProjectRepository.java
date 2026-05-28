package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.Project;
import com.buildlink.buildlink.entity.ProjectStatus;
import com.buildlink.buildlink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Projets d'un client
    List<Project> findByClient(User client);

    // Projets d'un architecte
    List<Project> findByArchitect(User architect);

    // Projets par statut
    List<Project> findByStatus(ProjectStatus status);

    // Projets d'un architecte par statut
    List<Project> findByArchitectAndStatus(User architect, ProjectStatus status);
}