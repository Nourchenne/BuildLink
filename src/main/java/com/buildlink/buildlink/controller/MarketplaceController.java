package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.*;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    private final ProjectService projectService;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 ProjectService projectService) {
        this.marketplaceService = marketplaceService;
        this.projectService = projectService;
    }

    // ─── ARCHITECTE ───────────────────────────────────────

    // Liste de mes demandes
    @GetMapping("/architect/requests")
    public String myRequests(Model model, Principal principal) {
        List<MaterialRequest> requests = marketplaceService.getArchitectRequests(principal.getName());
        model.addAttribute("requests", requests);
        return "architect/requests";
    }

    // Formulaire nouvelle demande
    @GetMapping("/architect/requests/new")
    public String newRequestForm(Model model, Principal principal) {
        model.addAttribute("requestDTO", new MaterialRequestDTO());
        model.addAttribute("projects", projectService.getArchitectProjects(principal.getName()));
        return "architect/request-form";
    }

    // Créer une demande
    @PostMapping("/architect/requests/new")
    public String createRequest(@Valid @ModelAttribute MaterialRequestDTO requestDTO,
                                BindingResult bindingResult,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projects", projectService.getArchitectProjects(principal.getName()));
            return "architect/request-form";
        }
        try {
            marketplaceService.createRequest(requestDTO, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Demande publiée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/requests";
    }

    // Détail d'une demande + offres reçues
    @GetMapping("/architect/requests/{id}")
    public String requestDetail(@PathVariable Long id, Model model, Principal principal,
                                RedirectAttributes redirectAttributes) {
        MaterialRequest request = marketplaceService.getRequestById(id);
        // ✅ Vérification que la demande appartient à l'architecte connecté
        if (!request.getArchitect().getEmail().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "Accès non autorisé à cette demande");
            return "redirect:/architect/requests";
        }
        List<Offer> offers = marketplaceService.getOffersForRequest(id);
        model.addAttribute("request", request);
        model.addAttribute("offers", offers);
        return "architect/request-detail";
    }

    // Accepter une offre
    @PostMapping("/architect/offers/{offerId}/accept")
    public String acceptOffer(@PathVariable Long offerId,
                              @RequestParam Long requestId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            // ✅ Vérification que la demande appartient à l'architecte connecté
            MaterialRequest request = marketplaceService.getRequestById(requestId);
            if (!request.getArchitect().getEmail().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("error", "Accès non autorisé");
                return "redirect:/architect/requests";
            }
            marketplaceService.acceptOffer(offerId);
            redirectAttributes.addFlashAttribute("success", "Offre acceptée ! La demande est fermée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/requests/" + requestId;
    }

    // Fermer une demande
    @PostMapping("/architect/requests/{id}/close")
    public String closeRequest(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        marketplaceService.closeRequest(id);
        redirectAttributes.addFlashAttribute("success", "Demande fermée.");
        return "redirect:/architect/requests";
    }

    // ─── FOURNISSEUR ──────────────────────────────────────

    // Toutes les demandes ouvertes
    @GetMapping("/supplier/marketplace")
    public String openRequests(Model model, Principal principal) {
        List<MaterialRequest> requests = marketplaceService.getOpenRequests();
        List<Offer> myOffers = marketplaceService.getSupplierOffers(principal.getName());
        
        // Créer un Set des IDs de demandes pour lesquelles le fournisseur a déjà envoyé une offre
        java.util.Set<Long> requestIdsWithOffers = myOffers.stream()
                .filter(o -> o.getRequest() != null)
                .map(o -> o.getRequest().getId())
                .collect(java.util.stream.Collectors.toSet());
        
        model.addAttribute("requests", requests);
        model.addAttribute("myOffers", myOffers);
        model.addAttribute("requestIdsWithOffers", requestIdsWithOffers);
        return "supplier/marketplace";
    }

    // Détail d'une demande + formulaire offre
    @GetMapping("/supplier/marketplace/{id}")
    public String requestDetailForSupplier(@PathVariable Long id, Model model,
                                           Principal principal) {
        MaterialRequest request = marketplaceService.getRequestById(id);
        Offer existingOffer = marketplaceService.getSupplierOfferForRequest(id, principal.getName());
        model.addAttribute("request", request);
        model.addAttribute("offerDTO", new OfferDTO());
        model.addAttribute("existingOffer", existingOffer);
        return "supplier/request-detail";
    }

    // Soumettre une offre
    @PostMapping("/supplier/marketplace/{id}/offer")
    public String submitOffer(@PathVariable Long id,
                              @Valid @ModelAttribute OfferDTO offerDTO,
                              BindingResult bindingResult,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("request", marketplaceService.getRequestById(id));
            return "supplier/request-detail";
        }
        try {
            marketplaceService.submitOffer(id, offerDTO, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Offre envoyée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/marketplace/" + id;
    }
}