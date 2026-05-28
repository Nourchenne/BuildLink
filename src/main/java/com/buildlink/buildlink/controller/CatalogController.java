package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.CatalogItemDTO;
import com.buildlink.buildlink.entity.CatalogItem;
import com.buildlink.buildlink.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ─── ARCHITECTE — Voir le catalogue ───────────────────

    @GetMapping("/architect/catalog")
    public String viewCatalog(@RequestParam(required = false) String search,
                              Model model) {
        List<CatalogItem> items = catalogService.search(search);
        model.addAttribute("items", items);
        model.addAttribute("search", search);
        return "architect/catalog";
    }

    // ─── FOURNISSEUR — Gérer son catalogue ────────────────

    @GetMapping("/supplier/catalog")
    public String supplierCatalog(Model model, Principal principal) {
        List<CatalogItem> items = catalogService.getSupplierCatalog(principal.getName());
        model.addAttribute("items", items);
        model.addAttribute("catalogItemDTO", new CatalogItemDTO());
        return "supplier/catalog";
    }

    @PostMapping("/supplier/catalog/add")
    public String addItem(@Valid @ModelAttribute CatalogItemDTO catalogItemDTO,
                          BindingResult bindingResult,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", catalogService.getSupplierCatalog(principal.getName()));
            return "supplier/catalog";
        }
        try {
            catalogService.addItem(catalogItemDTO, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Article ajouté au catalogue !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/catalog";
    }

    @PostMapping("/supplier/catalog/delete/{id}")
    public String deleteItem(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            catalogService.deleteItem(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Article supprimé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/catalog";
    }

    @PostMapping("/supplier/catalog/toggle/{id}")
    public String toggleItem(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            catalogService.toggleAvailability(id, principal.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/catalog";
    }
}