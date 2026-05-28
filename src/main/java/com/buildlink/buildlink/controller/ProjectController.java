package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.ProjectDTO;
import com.buildlink.buildlink.entity.Project;
import com.buildlink.buildlink.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/client/projects")
    public String clientProjects(Model model, Principal principal) {
        List<Project> projects = projectService.getClientProjects(principal.getName());
        model.addAttribute("projects", projects);
        return "client/projects";
    }

    @GetMapping("/client/projects/new")
    public String newProjectForm(@RequestParam(required = false) Long architectId,
                                 Model model) {
        com.buildlink.buildlink.dto.ProjectDTO dto = new com.buildlink.buildlink.dto.ProjectDTO();
        // ✅ Pré-sélection de l'architecte si on vient de la page de recherche
        if (architectId != null) {
            dto.setArchitectId(architectId);
        }
        model.addAttribute("projectDTO", dto);
        model.addAttribute("architects", projectService.getAllArchitects());
        return "client/project-form";
    }

    @PostMapping("/client/projects/new")
    public String createProject(@Valid @ModelAttribute ProjectDTO projectDTO,
                                BindingResult bindingResult,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("architects", projectService.getAllArchitects());
            return "client/project-form";
        }
        try {
            projectService.createProject(projectDTO, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Projet créé avec succès !");
            return "redirect:/client/projects";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/projects/new";
        }
    }

    // ✅ Vérification que le projet appartient au client connecté
    @GetMapping("/client/projects/{id}")
    public String clientProjectDetail(@PathVariable Long id, Model model, Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getClientProjectById(id, principal.getName());
            model.addAttribute("project", project);
            return "client/project-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/projects";
        }
    }

    @GetMapping("/architect/projects")
    public String architectProjects(Model model, Principal principal) {
        List<Project> projects = projectService.getArchitectProjects(principal.getName());
        model.addAttribute("projects", projects);
        return "architect/projects";
    }

    // ✅ Vérification que le projet appartient à l'architecte connecté
    @GetMapping("/architect/projects/{id}")
    public String architectProjectDetail(@PathVariable Long id, Model model, Principal principal,
                                         RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getArchitectProjectById(id, principal.getName());
            model.addAttribute("project", project);
            return "architect/project-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/architect/projects";
        }
    }

    // ✅ Vérification ownership + statut géré dans le service
    @PostMapping("/architect/projects/{id}/accept")
    public String acceptProject(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            // Vérifier que ce projet appartient bien à l'architecte connecté
            projectService.getArchitectProjectById(id, principal.getName());
            projectService.acceptProject(id);
            redirectAttributes.addFlashAttribute("success", "Projet accepté !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects";
    }

    // ✅ Vérification ownership + statut géré dans le service
    @PostMapping("/architect/projects/{id}/cancel")
    public String cancelProject(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            // Vérifier que ce projet appartient bien à l'architecte connecté
            projectService.getArchitectProjectById(id, principal.getName());
            projectService.cancelProject(id);
            redirectAttributes.addFlashAttribute("success", "Projet refusé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects";
    }
}