package com.buildlink.buildlink.service;

import com.buildlink.buildlink.dto.ProjectDTO;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.ProjectRepository;
import com.buildlink.buildlink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void createProject(ProjectDTO dto, String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        if (client.getRole() != Role.CLIENT) {
            throw new RuntimeException("Seul un client peut créer un projet");
        }

        User architect = userRepository.findById(dto.getArchitectId())
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));

        // Vérification que l'utilisateur sélectionné est bien un architecte
        if (architect.getRole() != Role.ARCHITECT) {
            throw new RuntimeException("L'utilisateur sélectionné n'est pas un architecte");
        }

        Project project = new Project();
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setLocation(dto.getLocation());
        project.setBudget(dto.getBudget());
        project.setClient(client);
        project.setArchitect(architect);

        projectRepository.save(project);
        log.info("Projet '{}' créé par {} et assigné à {}", dto.getTitle(), clientEmail, architect.getEmail());
        notificationService.notifyProjectReceived(architect, project);
    }

    public List<Project> getClientProjects(String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return projectRepository.findByClient(client);
    }

    public List<Project> getArchitectProjects(String architectEmail) {
        User architect = userRepository.findByEmail(architectEmail)
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));
        return projectRepository.findByArchitect(architect);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
    }

    // Vérification que le projet appartient à l'architecte et au statut INQUIRY
    @Transactional
    public void acceptProject(Long projectId) {
        Project project = getProjectById(projectId);
        if (project.getStatus() != ProjectStatus.INQUIRY) {
            throw new RuntimeException("Seuls les projets en attente (INQUIRY) peuvent être acceptés");
        }
        project.setStatus(ProjectStatus.PLANNING);
        projectRepository.save(project);
        log.info("Projet {} accepté par l'architecte {}", projectId, project.getArchitect().getEmail());
        notificationService.notifyProjectAccepted(project.getClient(), project);
    }

    // Vérification du statut avant annulation
    @Transactional
    public void cancelProject(Long projectId) {
        Project project = getProjectById(projectId);
        if (project.getStatus() == ProjectStatus.COMPLETED
                || project.getStatus() == ProjectStatus.CANCELLED) {
            throw new RuntimeException("Ce projet ne peut plus être annulé");
        }
        project.setStatus(ProjectStatus.CANCELLED);
        projectRepository.save(project);
        log.info("Projet {} annulé", projectId);
        notificationService.notifyProjectCancelled(project.getClient(), project);
    }

    // Utilise la query par rôle au lieu de charger tous les users
    public List<User> getAllArchitects() {
        return userRepository.findByRole(Role.ARCHITECT);
    }

    // Vérification que le projet appartient bien au client
    public Project getClientProjectById(Long projectId, String clientEmail) {
        Project project = getProjectById(projectId);
        if (!project.getClient().getEmail().equals(clientEmail)) {
            throw new RuntimeException("Accès non autorisé à ce projet");
        }
        return project;
    }

    // Vérification que le projet appartient bien à l'architecte
    public Project getArchitectProjectById(Long projectId, String architectEmail) {
        Project project = getProjectById(projectId);
        if (!project.getArchitect().getEmail().equals(architectEmail)) {
            throw new RuntimeException("Accès non autorisé à ce projet");
        }
        return project;
    }
}