package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.service.InvoiceService;
import com.buildlink.buildlink.service.ProjectService;
import com.buildlink.buildlink.service.MaterialService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ProjectService projectService;
    private final MaterialService materialService;

    public InvoiceController(InvoiceService invoiceService,
                             ProjectService projectService,
                             MaterialService materialService) {
        this.invoiceService = invoiceService;
        this.projectService = projectService;
        this.materialService = materialService;
    }

    // ─── ARCHITECTE ───────────────────────────────────────

    // Générer la facture (après PLAN_APPROVED)
    @PostMapping("/architect/projects/{id}/invoice/generate")
    public String generateInvoice(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            invoiceService.generateInvoice(id);
            redirectAttributes.addFlashAttribute("success",
                    "Facture générée et envoyée au client !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/architect/projects/" + id;
    }

    // Voir la facture (architecte)
    @GetMapping("/architect/projects/{id}/invoice")
    public String architectViewInvoice(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        Invoice invoice = invoiceService.getByProject(id);
        List<PurchaseOrder> orders = invoiceService.getOrdersByProject(id);
        
        // ✅ Séparer matériaux avec/sans fournisseur
        List<MaterialItem> allMaterials = materialService.getByProject(id);
        List<MaterialItem> orderedItems = allMaterials.stream()
                .filter(m -> m.getSupplier() != null)
                .collect(Collectors.toList());
        List<MaterialItem> toSourceItems = allMaterials.stream()
                .filter(m -> m.getSupplier() == null)
                .collect(Collectors.toList());

        model.addAttribute("project", project);
        model.addAttribute("invoice", invoice);
        model.addAttribute("orders", orders);
        model.addAttribute("orderedItems", orderedItems);
        model.addAttribute("toSourceItems", toSourceItems);
        return "architect/invoice";
    }

    // ─── CLIENT ───────────────────────────────────────────

    // Voir la facture (client)
    @GetMapping("/client/projects/{id}/invoice")
    public String clientViewInvoice(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        Invoice invoice = invoiceService.getByProject(id);
        
        // ✅ Séparer matériaux avec/sans fournisseur
        List<MaterialItem> allMaterials = materialService.getByProject(id);
        List<MaterialItem> orderedItems = allMaterials.stream()
                .filter(m -> m.getSupplier() != null)
                .collect(Collectors.toList());
        List<MaterialItem> toSourceItems = allMaterials.stream()
                .filter(m -> m.getSupplier() == null)
                .collect(Collectors.toList());

        model.addAttribute("project", project);
        model.addAttribute("invoice", invoice);
        model.addAttribute("orderedItems", orderedItems);
        model.addAttribute("toSourceItems", toSourceItems);
        return "client/invoice";
    }

    // Client approuve la facture
    @PostMapping("/client/invoice/{invoiceId}/approve")
    public String approveInvoice(@PathVariable Long invoiceId,
                                 @RequestParam Long projectId,
                                 RedirectAttributes redirectAttributes) {
        try {
            invoiceService.approveInvoice(invoiceId);
            redirectAttributes.addFlashAttribute("success",
                    "Facture approuvée ! Les bons de commande ont été créés.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/projects/" + projectId;
    }

    // Client refuse la facture
    @PostMapping("/client/invoice/{invoiceId}/reject")
    public String rejectInvoice(@PathVariable Long invoiceId,
                                @RequestParam Long projectId,
                                RedirectAttributes redirectAttributes) {
        try {
            invoiceService.rejectInvoice(invoiceId);
            redirectAttributes.addFlashAttribute("info",
                    "Facture refusée. L'architecte sera notifié.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/projects/" + projectId;
    }

    // ─── FOURNISSEUR ──────────────────────────────────────

    // Mes bons de commande
    @GetMapping("/supplier/orders")
    public String supplierOrders(Model model, Principal principal) {
        List<PurchaseOrder> orders = invoiceService.getOrdersBySupplier(
                principal.getName());
        model.addAttribute("orders", orders);
        return "supplier/orders";
    }

    // Mettre à jour le statut d'une commande (FOURNISSEUR)
    @PostMapping("/supplier/orders/{orderId}/status")
    public String updateOrderStatus(@PathVariable Long orderId,
                                    @RequestParam OrderStatus status,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Vérifier que cette commande appartient au fournisseur connecté
            List<PurchaseOrder> supplierOrders = invoiceService.getOrdersBySupplier(
                    principal.getName());
            boolean isOwner = supplierOrders.stream()
                    .anyMatch(o -> o.getId().equals(orderId));
            if (!isOwner) {
                redirectAttributes.addFlashAttribute("error", "Accès non autorisé");
                return "redirect:/supplier/orders";
            }
            invoiceService.updateOrderStatus(orderId, status);
            String label = switch (status) {
                case CONFIRMED -> "✅ Commande confirmée !";
                case PREPARING -> "🔧 Commande en préparation !";
                case SHIPPED   -> "🚛 Commande expédiée ! Le client doit maintenant confirmer la réception.";
                case REJECTED  -> "❌ Commande refusée !";
                default        -> "Statut mis à jour !";
            };
            redirectAttributes.addFlashAttribute("success", label);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/orders";
    }

    // ═══════════════════════════════════════════════════════════════
    // CLIENT - Voir et confirmer les commandes de ses projets
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/client/orders")
    public String clientOrders(Principal principal, Model model) {
        List<PurchaseOrder> orders = invoiceService.getOrdersByClient(principal.getName());
        model.addAttribute("orders", orders);
        return "client/orders";
    }

    // Page dédiée aux livraisons pour les clients
    @GetMapping("/client/deliveries")
    public String clientDeliveries(@RequestParam(required = false) String status,
                                   Principal principal,
                                   Model model) {
        List<PurchaseOrder> allOrders = invoiceService.getOrdersByClient(principal.getName());
        
        // Filtrer par statut si spécifié
        List<PurchaseOrder> orders;
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus filterStatus = OrderStatus.valueOf(status);
                orders = allOrders.stream()
                        .filter(o -> o.getStatus() == filterStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                orders = allOrders;
            }
        } else {
            orders = allOrders;
        }
        
        model.addAttribute("orders", orders);
        return "client/deliveries";
    }

    // Client confirme la réception d'une commande
    @PostMapping("/client/orders/{orderId}/confirm-delivery")
    public String confirmDelivery(@PathVariable Long orderId,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Vérifier que cette commande appartient au client
            List<PurchaseOrder> clientOrders = invoiceService.getOrdersByClient(principal.getName());
            boolean isOwner = clientOrders.stream()
                    .anyMatch(o -> o.getId().equals(orderId));
            if (!isOwner) {
                redirectAttributes.addFlashAttribute("error", "Accès non autorisé");
                return "redirect:/client/deliveries";
            }
            invoiceService.confirmDeliveryByClient(orderId);
            redirectAttributes.addFlashAttribute("success", 
                "📦 Réception confirmée ! Le fournisseur et l'architecte ont été notifiés.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/deliveries";
    }
}