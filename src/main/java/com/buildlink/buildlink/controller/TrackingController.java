package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import com.buildlink.buildlink.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class TrackingController {

    private final ProjectService projectService;
    private final MaterialService materialService;
    private final InvoiceService invoiceService;
    private final MarketplaceService marketplaceService;

    public TrackingController(ProjectService projectService,
                              MaterialService materialService,
                              InvoiceService invoiceService,
                              MarketplaceService marketplaceService) {
        this.projectService = projectService;
        this.materialService = materialService;
        this.invoiceService = invoiceService;
        this.marketplaceService = marketplaceService;
    }

    // ─── CLIENT ───────────────────────────────────────────
    @GetMapping("/client/tracking")
    public String clientTracking(Model model, Principal principal) {
        List<Project> allProjects =
                projectService.getClientProjects(principal.getName());

        // Projets à approuver (plan envoyé)
        List<Project> planToApprove = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLAN_SENT)
                .collect(Collectors.toList());

        // Factures à approuver
        List<Project> invoiceToApprove = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.INVOICE_SENT)
                .collect(Collectors.toList());

        // Projets en cours de livraison
        List<Project> inDelivery = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.ORDERED)
                .collect(Collectors.toList());

        // Projets en attente d'architecte
        List<Project> waiting = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.INQUIRY
                        || p.getStatus() == ProjectStatus.PLANNING)
                .collect(Collectors.toList());

        // Projets terminés
        List<Project> completed = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED)
                .collect(Collectors.toList());

        model.addAttribute("allProjects", allProjects);
        model.addAttribute("planToApprove", planToApprove);
        model.addAttribute("invoiceToApprove", invoiceToApprove);
        model.addAttribute("inDelivery", inDelivery);
        model.addAttribute("waiting", waiting);
        model.addAttribute("completed", completed);
        model.addAttribute("totalActions",
                planToApprove.size() + invoiceToApprove.size());
        return "client/tracking";
    }

    // ─── ARCHITECTE ───────────────────────────────────────
    @GetMapping("/architect/tracking")
    public String architectTracking(Model model, Principal principal) {
        List<Project> allProjects =
                projectService.getArchitectProjects(principal.getName());

        // Nouvelles demandes à traiter
        List<Project> newRequests = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.INQUIRY)
                .collect(Collectors.toList());

        // Plans à préparer
        List<Project> toPlan = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLANNING)
                .collect(Collectors.toList());

        // Plans envoyés, en attente client
        List<Project> planWaiting = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLAN_SENT)
                .collect(Collectors.toList());

        // Plans approuvés → générer facture
        List<Project> toInvoice = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLAN_APPROVED)
                .collect(Collectors.toList());

        // Factures en attente client
        List<Project> invoiceWaiting = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.INVOICE_SENT)
                .collect(Collectors.toList());

        // Commandes en cours
        List<Project> ordered = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.ORDERED)
                .collect(Collectors.toList());

        // Demandes marketplace ouvertes
        List<MaterialRequest> openRequests =
                marketplaceService.getArchitectRequests(principal.getName())
                        .stream()
                        .filter(r -> r.getStatus() == RequestStatus.OPEN)
                        .collect(Collectors.toList());

        // Offres reçues non traitées
        long pendingOffers = openRequests.stream()
                .mapToLong(r -> marketplaceService
                        .getOffersForRequest(r.getId())
                        .stream()
                        .filter(o -> o.getStatus() == OfferStatus.PENDING)
                        .count())
                .sum();

        model.addAttribute("allProjects", allProjects);
        model.addAttribute("newRequests", newRequests);
        model.addAttribute("toPlan", toPlan);
        model.addAttribute("planWaiting", planWaiting);
        model.addAttribute("toInvoice", toInvoice);
        model.addAttribute("invoiceWaiting", invoiceWaiting);
        model.addAttribute("ordered", ordered);
        model.addAttribute("openRequests", openRequests);
        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("totalActions",
                newRequests.size() + toPlan.size() + toInvoice.size());
        return "architect/tracking";
    }

    // ─── FOURNISSEUR ──────────────────────────────────────
    @GetMapping("/supplier/tracking")
    public String supplierTracking(Model model, Principal principal) {
        List<Offer> myOffers =
                marketplaceService.getSupplierOffers(principal.getName());

        List<PurchaseOrder> myOrders =
                invoiceService.getOrdersBySupplier(principal.getName());

        // Offres en attente de réponse
        List<Offer> pendingOffers = myOffers.stream()
                .filter(o -> o.getStatus() == OfferStatus.PENDING)
                .collect(Collectors.toList());

        // Offres acceptées
        List<Offer> acceptedOffers = myOffers.stream()
                .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
                .collect(Collectors.toList());

        // Commandes à confirmer
        List<PurchaseOrder> toConfirm = myOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());

        // Commandes à préparer
        List<PurchaseOrder> toPrepare = myOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .collect(Collectors.toList());

        // Commandes expédiées
        List<PurchaseOrder> shipped = myOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.SHIPPED)
                .collect(Collectors.toList());

        // Commandes livrées
        List<PurchaseOrder> delivered = myOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());

        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("acceptedOffers", acceptedOffers);
        model.addAttribute("toConfirm", toConfirm);
        model.addAttribute("toPrepare", toPrepare);
        model.addAttribute("shipped", shipped);
        model.addAttribute("delivered", delivered);
        model.addAttribute("totalOrders", myOrders.size());
        model.addAttribute("totalActions",
                toConfirm.size() + toPrepare.size());
        return "supplier/tracking";
    }

    // ─── TRACKING DÉTAILLÉ PAR PROJET (ARCHITECTE) ───────
    @GetMapping("/architect/projects/{id}/tracking")
    public String projectTracking(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        List<MaterialItem> materials = materialService.getByProject(id);
        List<PurchaseOrder> orders = invoiceService.getOrdersByProject(id);
        
        // Créer une map matériau → commande
        Map<Long, PurchaseOrder> materialOrderMap = new HashMap<>();
        for (PurchaseOrder order : orders) {
            if (order.getMaterialItem() != null) {
                materialOrderMap.put(order.getMaterialItem().getId(), order);
            }
        }
        
        model.addAttribute("project", project);
        model.addAttribute("materials", materials);
        model.addAttribute("materialOrderMap", materialOrderMap);
        return "architect/project-tracking";
    }
}