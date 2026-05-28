package com.buildlink.buildlink.service;

import com.buildlink.buildlink.dto.MaterialItemDTO;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.MaterialItemRepository;
import com.buildlink.buildlink.repository.ProjectRepository;
import com.buildlink.buildlink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialService {

    private static final Logger log = LoggerFactory.getLogger(MaterialService.class);

    private final MaterialItemRepository materialItemRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public MaterialService(MaterialItemRepository materialItemRepository,
                           ProjectRepository projectRepository,
                           NotificationService notificationService,
                           UserRepository userRepository) {
        this.materialItemRepository = materialItemRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public List<MaterialItem> getByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return materialItemRepository.findByProject(project);
    }

    public void addMaterial(Long projectId, MaterialItemDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // ✅ On peut ajouter des matériaux uniquement si le projet est en planification
        if (project.getStatus() != ProjectStatus.PLANNING
                && project.getStatus() != ProjectStatus.PLAN_SENT) {
            throw new RuntimeException("Impossible d'ajouter des matériaux dans l'état actuel du projet");
        }

        MaterialItem item = new MaterialItem();
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setDescription(dto.getDescription());
        item.setQuantity(dto.getQuantity());
        item.setUnit(dto.getUnit());
        item.setEstimatedPrice(dto.getEstimatedPrice());
        item.setProject(project);

        // ✅ Associer le fournisseur si fourni
        if (dto.getSupplierId() != null) {
            userRepository.findById(dto.getSupplierId()).ifPresent(item::setSupplier);
        }

        materialItemRepository.save(item);
        log.info("Matériau '{}' ajouté au projet {}", dto.getName(), projectId);
    }

    public void deleteMaterial(Long itemId, Long projectId) {
        MaterialItem item = materialItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Matériau non trouvé"));
        if (!item.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Cet élément n'appartient pas à ce projet");
        }
        materialItemRepository.deleteById(itemId);
        log.info("Matériau {} supprimé du projet {}", itemId, projectId);
    }

    @Transactional
    public void sendPlan(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // ✅ Vérification du statut avant envoi du plan
        if (project.getStatus() != ProjectStatus.PLANNING
                && project.getStatus() != ProjectStatus.PLAN_SENT) {
            throw new RuntimeException("Le plan ne peut être envoyé que depuis un projet en planification");
        }

        List<MaterialItem> items = materialItemRepository.findByProject(project);
        if (items.isEmpty()) {
            throw new RuntimeException("Ajoutez au moins un matériau avant d'envoyer le plan");
        }

        project.setStatus(ProjectStatus.PLAN_SENT);
        projectRepository.save(project);
        log.info("Plan envoyé au client pour le projet {}", projectId);
        notificationService.notifyPlanSent(project.getClient(), project);
    }

    @Transactional
    public void approvePlan(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // ✅ Vérification que le plan a été envoyé avant de pouvoir l'approuver
        if (project.getStatus() != ProjectStatus.PLAN_SENT) {
            throw new RuntimeException("Seul un plan envoyé peut être approuvé");
        }

        project.setStatus(ProjectStatus.PLAN_APPROVED);
        projectRepository.save(project);
        log.info("Plan approuvé par le client pour le projet {}", projectId);
        notificationService.notifyPlanApproved(project.getArchitect(), project);
    }

    @Transactional
    public void rejectPlan(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // ✅ Vérification que le plan a été envoyé avant de pouvoir le refuser
        if (project.getStatus() != ProjectStatus.PLAN_SENT) {
            throw new RuntimeException("Seul un plan envoyé peut être refusé");
        }

        project.setStatus(ProjectStatus.PLANNING);
        projectRepository.save(project);
        log.info("Plan refusé par le client pour le projet {} — retour en planification", projectId);
        notificationService.notifyPlanRejected(project.getArchitect(), project);
    }

    public double calculateTotal(Long projectId) {
        return getByProject(projectId).stream()
                .mapToDouble(item -> item.getQuantity() * item.getEstimatedPrice())
                .sum();
    }
}