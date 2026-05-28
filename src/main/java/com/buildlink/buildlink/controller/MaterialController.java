package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.MaterialItemDTO;
import com.buildlink.buildlink.entity.MaterialItem;
import com.buildlink.buildlink.entity.Project;
import com.buildlink.buildlink.entity.Role;
import com.buildlink.buildlink.entity.User;
import com.buildlink.buildlink.repository.UserRepository;
import com.buildlink.buildlink.service.MaterialService;
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
public class MaterialController {

    private final MaterialService materialService;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public MaterialController(MaterialService materialService,
                              ProjectService projectService,
                              UserRepository userRepository) {
        this.materialService = materialService;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    // ─── ARCHITECTE ───────────────────────────────────────

    // Page plan de matériaux (architecte)
    @GetMapping("/architect/projects/{id}/plan")
    public String planPage(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        List<MaterialItem> items = materialService.getByProject(id);
        double total = materialService.calculateTotal(id);
        List<User> suppliers = userRepository.findByRole(Role.SUPPLIER);

        model.addAttribute("project", project);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("materialDTO", new MaterialItemDTO());
        return "architect/plan";
    }

    // Ajouter un matériau
    @PostMapping("/architect/projects/{id}/plan/add")
    public String addMaterial(@PathVariable Long id,
                              @Valid @ModelAttribute MaterialItemDTO materialDTO,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "Données invalides");
            return "redirect:/architect/projects/" + id + "/plan";
        }
        try {
            materialService.addMaterial(id, materialDTO);
            redirectAttributes.addFlashAttribute("success", "Matériau ajouté !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects/" + id + "/plan";
    }

    // Supprimer un matériau
    @PostMapping("/architect/projects/{id}/plan/delete/{itemId}")
    public String deleteMaterial(@PathVariable Long id,
                                 @PathVariable Long itemId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getProjectById(id);
            if (!project.getArchitect().getEmail().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("error", "Accès non autorisé");
                return "redirect:/architect/projects";
            }
            materialService.deleteMaterial(itemId, id);
            redirectAttributes.addFlashAttribute("success", "Matériau supprimé !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects/" + id + "/plan";
    }

    // Envoyer le plan au client
    @PostMapping("/architect/projects/{id}/plan/send")
    public String sendPlan(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            materialService.sendPlan(id);
            redirectAttributes.addFlashAttribute("success", "Plan envoyé au client !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects/" + id + "/plan";
    }

    // ─── CLIENT ───────────────────────────────────────────

    // Voir le plan (client)
    @GetMapping("/client/projects/{id}/plan")
    public String clientPlanPage(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        List<MaterialItem> items = materialService.getByProject(id);
        double total = materialService.calculateTotal(id);

        model.addAttribute("project", project);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "client/plan";
    }

    // Client approuve le plan
    @PostMapping("/client/projects/{id}/plan/approve")
    public String approvePlan(@PathVariable Long id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            // ✅ Vérification ownership avant approbation
            Project project = projectService.getClientProjectById(id, principal.getName());
            materialService.approvePlan(project.getId());
            redirectAttributes.addFlashAttribute("success", "Plan approuvé !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/projects/" + id;
    }

    // Client refuse le plan
    @PostMapping("/client/projects/{id}/plan/reject")
    public String rejectPlan(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            // ✅ Vérification ownership avant refus
            Project project = projectService.getClientProjectById(id, principal.getName());
            materialService.rejectPlan(project.getId());
            redirectAttributes.addFlashAttribute("info", "Plan renvoyé à l'architecte pour révision.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/projects/" + id;
    }
}