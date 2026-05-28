package com.buildlink.buildlink.service;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository orderRepository;
    private final ProjectRepository projectRepository;
    private final MaterialItemRepository materialItemRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PurchaseOrderRepository orderRepository,
                          ProjectRepository projectRepository,
                          MaterialItemRepository materialItemRepository,
                          NotificationService notificationService,
                          UserRepository userRepository,
                          OfferRepository offerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.projectRepository = projectRepository;
        this.materialItemRepository = materialItemRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public Invoice generateInvoice(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // ✅ Vérification que le statut est PLAN_APPROVED avant génération
        if (project.getStatus() != ProjectStatus.PLAN_APPROVED) {
            throw new RuntimeException("La facture ne peut être générée qu'après approbation du plan par le client");
        }

        List<MaterialItem> items = materialItemRepository.findByProject(project);
        if (items.isEmpty()) {
            throw new RuntimeException("Aucun matériau dans le plan");
        }

        double total = items.stream()
                .mapToDouble(i -> i.getQuantity() * i.getEstimatedPrice())
                .sum();

        if (invoiceRepository.existsByProject(project)) {
            Invoice existing = invoiceRepository.findByProject(project).get();
            if (existing.getStatus() == InvoiceStatus.PENDING) {
                return existing;
            }
            // ✅ Si la facture avait été refusée, on la réinitialise (l'architecte régénère)
            if (existing.getStatus() == InvoiceStatus.REJECTED) {
                existing.setTotalAmount(total);
                existing.setStatus(InvoiceStatus.PENDING);
                project.setStatus(ProjectStatus.INVOICE_SENT);
                projectRepository.save(project);
                Invoice saved = invoiceRepository.save(existing);
                log.info("Facture régénérée pour le projet {} après rejet — total: {}", projectId, total);
                notificationService.notifyInvoiceSent(project.getClient(), project);
                return saved;
            }
            throw new RuntimeException("Une facture a déjà été traitée pour ce projet");
        }

        Invoice invoice = new Invoice();
        invoice.setProject(project);
        invoice.setTotalAmount(total);

        project.setStatus(ProjectStatus.INVOICE_SENT);
        projectRepository.save(project);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Facture générée pour le projet {} — total: {}", projectId, total);
        notificationService.notifyInvoiceSent(project.getClient(), project);
        return saved;
    }

    public Invoice getByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return invoiceRepository.findByProject(project)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
    }

    public Invoice getById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
    }

    @Transactional
    public void approveInvoice(Long invoiceId) {
        Invoice invoice = getById(invoiceId);

        // ✅ Vérification statut facture
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new RuntimeException("Cette facture a déjà été traitée");
        }

        invoice.setStatus(InvoiceStatus.APPROVED);
        invoiceRepository.save(invoice);

        Project project = invoice.getProject();
        project.setStatus(ProjectStatus.ORDERED);
        projectRepository.save(project);

        // ✅ Récupérer les offres acceptées (pour la compatibilité marketplace)
        List<Offer> acceptedOffers = offerRepository.findByRequest_ProjectAndStatus(
                project, OfferStatus.ACCEPTED);

        // Construire un index fournisseur par défaut depuis les offres marketplace
        User marketplaceSupplier = acceptedOffers.isEmpty() ? null
                : acceptedOffers.get(0).getSupplier();

        List<MaterialItem> items = materialItemRepository.findByProject(project);
        Set<Long> notifiedSuppliers = new HashSet<>();
        
        // ✅ Séparer les items : avec fournisseur (commande immédiate) vs sans fournisseur (à sourcer)
        int orderedCount = 0;
        int toSourceCount = 0;

        for (MaterialItem item : items) {
            // ✅ Priorité : fournisseur directement associé au matériau (via le plan)
            User supplierForItem = item.getSupplier() != null ? item.getSupplier() : marketplaceSupplier;

            // ✅ Créer la commande UNIQUEMENT si un fournisseur est disponible
            if (supplierForItem != null) {
                PurchaseOrder order = new PurchaseOrder();
                order.setProject(project);
                order.setMaterialItem(item);
                order.setQuantity(item.getQuantity());
                order.setUnitPrice(item.getEstimatedPrice());
                order.setTotalPrice(item.getQuantity() * item.getEstimatedPrice());
                order.setSupplier(supplierForItem);
                // ✅ Adresse de livraison = localisation du projet (chantier)
                order.setDeliveryAddress(project.getLocation());
                orderRepository.save(order);
                orderedCount++;

                // ✅ Notifier chaque fournisseur unique une seule fois
                if (notifiedSuppliers.add(supplierForItem.getId())) {
                    notificationService.create(supplierForItem, NotificationType.ORDER_CREATED,
                            "Nouveaux bons de commande 📦",
                            "Des bons de commande ont été créés pour le projet \""
                                    + project.getTitle() + "\".",
                            "/supplier/orders");
                }
            } else {
                // ✅ Item sans fournisseur : à sourcer manuellement par l'architecte
                toSourceCount++;
                log.info("Matériau '{}' du projet {} sans fournisseur — À sourcer ultérieurement",
                        item.getName(), project.getId());
            }
        }

        log.info("Facture {} approuvée — {} bons de commande créés, {} matériaux à sourcer pour le projet {}",
                invoiceId, orderedCount, toSourceCount, project.getId());
        notificationService.notifyInvoiceApproved(project.getArchitect(), project);

        // Notifier aussi les fournisseurs des offres marketplace acceptées (si pas déjà notifiés)
        for (Offer offer : acceptedOffers) {
            User supplier = offer.getSupplier();
            if (notifiedSuppliers.add(supplier.getId())) {
                notificationService.create(supplier, NotificationType.ORDER_CREATED,
                        "Nouveaux bons de commande 📦",
                        "Des bons de commande ont été créés pour le projet \""
                                + project.getTitle() + "\".",
                        "/supplier/orders");
            }
        }
    }

    @Transactional
    public void rejectInvoice(Long invoiceId) {
        Invoice invoice = getById(invoiceId);

        // ✅ Vérification statut
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new RuntimeException("Cette facture a déjà été traitée");
        }

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoiceRepository.save(invoice);

        Project project = invoice.getProject();
        project.setStatus(ProjectStatus.PLAN_APPROVED);
        projectRepository.save(project);

        log.info("Facture {} refusée par le client", invoiceId);
        notificationService.notifyInvoiceRejected(project.getArchitect(), project);
    }

    public List<PurchaseOrder> getOrdersByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return orderRepository.findByProject(project);
    }

    public List<PurchaseOrder> getOrdersBySupplier(String email) {
        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));

        // ✅ Utiliser directement findBySupplier si le supplier est assigné aux orders
        List<PurchaseOrder> directOrders = orderRepository.findBySupplier(supplier);
        if (!directOrders.isEmpty()) {
            return directOrders;
        }

        // Fallback : trouver via les offres acceptées
        List<Offer> acceptedOffers = offerRepository.findBySupplierAndStatus(
                supplier, OfferStatus.ACCEPTED);
        List<Project> projects = acceptedOffers.stream()
                .filter(o -> o.getRequest().getProject() != null)
                .map(o -> o.getRequest().getProject())
                .distinct()
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            return List.of();
        }
        return orderRepository.findByProjectIn(projects);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        // ✅ Vérification de la progression logique des statuts
        OrderStatus current = order.getStatus();
        
        // ✅ MODIFICATION: Le fournisseur ne peut plus mettre à DELIVERED
        // SHIPPED peut maintenant seulement être changé par le CLIENT
        boolean valid = (current == OrderStatus.PENDING    && (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.REJECTED))
                     || (current == OrderStatus.CONFIRMED  && newStatus == OrderStatus.PREPARING)
                     || (current == OrderStatus.PREPARING  && newStatus == OrderStatus.SHIPPED);
        
        if (!valid) {
            throw new RuntimeException("Transition de statut invalide : " + current + " → " + newStatus);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Commande {} mise à jour par FOURNISSEUR : {} → {}", orderId, current, newStatus);

        // ✅ Message de notification lisible selon le nouveau statut
        String statusLabel;
        switch (newStatus) {
            case CONFIRMED  -> statusLabel = "✅ Confirmé";
            case PREPARING  -> statusLabel = "🔧 En préparation";
            case SHIPPED    -> statusLabel = "🚛 Expédié - En attente de confirmation client";
            case REJECTED   -> statusLabel = "❌ Refusé";
            default         -> statusLabel = newStatus.name();
        }

        Project project = order.getProject();
        
        // Si la commande est refusée, notification spéciale
        if (newStatus == OrderStatus.REJECTED) {
            // Notifier l'architecte avec un message d'action
            notificationService.create(project.getArchitect(), NotificationType.ORDER_STATUS_UPDATED,
                    "⚠️ Commande refusée par le fournisseur",
                    "Le fournisseur " + order.getSupplier().getFullName() + " a refusé la commande \"" 
                            + order.getMaterialItem().getName() + "\" (projet : " + project.getTitle() 
                            + "). Vous devez trouver un nouveau fournisseur pour ce matériau.",
                    "/architect/projects/" + project.getId() + "/tracking");
            
            // Notifier le client
            notificationService.create(project.getClient(), NotificationType.ORDER_STATUS_UPDATED,
                    "⚠️ Commande refusée",
                    "Une commande du projet \"" + project.getTitle() + "\" a été refusée par le fournisseur. "
                            + "L'architecte va trouver un nouveau fournisseur.",
                    "/client/projects/" + project.getId());
            
            log.warn("Commande {} REFUSÉE par le fournisseur {}. Architecte doit sourcer un nouveau fournisseur.", 
                    orderId, order.getSupplier().getFullName());
            return;
        }

        // ✅ Si SHIPPED : notifier le CLIENT qu'il doit confirmer la réception
        if (newStatus == OrderStatus.SHIPPED) {
            notificationService.create(project.getClient(), NotificationType.ORDER_STATUS_UPDATED,
                    "🚛 Commande expédiée - Action requise",
                    "La commande \"" + order.getMaterialItem().getName()
                            + "\" (projet : " + project.getTitle() + ") a été expédiée. "
                            + "Veuillez confirmer la réception quand les matériaux arrivent sur le chantier.",
                    "/client/orders");
            
            notificationService.create(project.getArchitect(), NotificationType.ORDER_STATUS_UPDATED,
                    "Commande expédiée " + statusLabel,
                    "La commande \"" + order.getMaterialItem().getName()
                            + "\" (projet : " + project.getTitle() + ") a été expédiée. "
                            + "Le client confirmera la réception.",
                    "/architect/tracking");
            return;
        }

        // Notifications normales pour CONFIRMED et PREPARING
        notificationService.create(project.getClient(), NotificationType.ORDER_STATUS_UPDATED,
                "Commande mise à jour " + statusLabel,
                "La commande \"" + order.getMaterialItem().getName()
                        + "\" (projet : " + project.getTitle() + ") est maintenant : " + statusLabel,
                "/client/tracking");

        notificationService.create(project.getArchitect(), NotificationType.ORDER_STATUS_UPDATED,
                "Commande mise à jour " + statusLabel,
                "La commande \"" + order.getMaterialItem().getName()
                        + "\" (projet : " + project.getTitle() + ") est maintenant : " + statusLabel,
                "/architect/tracking");
    }

    // ═══════════════════════════════════════════════════════════════
    // CLIENT confirme la livraison (DELIVERED)
    // ═══════════════════════════════════════════════════════════════
    
    @Transactional
    public void confirmDeliveryByClient(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        // ✅ Vérification : doit être en statut SHIPPED
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("La commande doit être expédiée avant de confirmer la réception (statut actuel : " 
                    + order.getStatus() + ")");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        Project project = order.getProject();
        log.info("Commande {} confirmée DELIVERED par le CLIENT du projet {}", orderId, project.getId());

        // Notifier le fournisseur
        notificationService.create(order.getSupplier(), NotificationType.ORDER_STATUS_UPDATED,
                "📦 Livraison confirmée par le client",
                "Le client a confirmé la réception de la commande \"" + order.getMaterialItem().getName()
                        + "\" (projet : " + project.getTitle() + "). La livraison est terminée !",
                "/supplier/orders");

        // Notifier l'architecte
        notificationService.create(project.getArchitect(), NotificationType.ORDER_STATUS_UPDATED,
                "📦 Livraison confirmée",
                "Le client a confirmé la réception de \"" + order.getMaterialItem().getName()
                        + "\" (projet : " + project.getTitle() + ").",
                "/architect/tracking");

        // ✅ Auto-complétion si toutes les commandes sont livrées
        List<PurchaseOrder> allOrders = orderRepository.findByProject(project);
        boolean allDelivered = allOrders.stream()
                .allMatch(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.REJECTED);
        if (allDelivered && !allOrders.isEmpty()) {
            project.setStatus(ProjectStatus.COMPLETED);
            projectRepository.save(project);
            log.info("Projet {} automatiquement complété — toutes commandes livrées", project.getId());
            notificationService.notifyProjectCompleted(project.getClient(), project);
        }
    }

    public List<PurchaseOrder> getOrdersByClient(String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        
        // Trouver tous les projets du client
        List<Project> projects = projectRepository.findByClient(client);
        
        // Retourner toutes les commandes de ces projets
        return orderRepository.findByProjectIn(projects);
    }
}

