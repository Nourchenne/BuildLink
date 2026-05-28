package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.ReviewDTO;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final ProjectService projectService;

    public ReviewController(ReviewService reviewService,
                            ProjectService projectService) {
        this.reviewService = reviewService;
        this.projectService = projectService;
    }

    // Formulaire avis (client)
    @GetMapping("/client/projects/{id}/review")
    public String reviewForm(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        Review existing = reviewService.getByProject(id);

        model.addAttribute("project", project);
        model.addAttribute("reviewDTO", new ReviewDTO());
        model.addAttribute("existingReview", existing);
        return "client/review";
    }

    // Soumettre un avis (client)
    @PostMapping("/client/projects/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @Valid @ModelAttribute ReviewDTO reviewDTO,
                               BindingResult bindingResult,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("project", projectService.getProjectById(id));
            model.addAttribute("existingReview", reviewService.getByProject(id));
            return "client/review";
        }
        try {
            reviewService.leaveReview(id, reviewDTO, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Merci pour votre avis !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/projects/" + id;
    }

    // Clôturer un projet (architecte)
    @PostMapping("/architect/projects/{id}/complete")
    public String completeProject(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            reviewService.completeProject(id);
            redirectAttributes.addFlashAttribute("success",
                    "Projet clôturé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects/" + id;
    }
}