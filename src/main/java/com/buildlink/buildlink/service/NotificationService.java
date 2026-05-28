package com.buildlink.buildlink.service;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Créer une notification
    public void create(User user, NotificationType type,
                       String title, String message, String link) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setLink(link);
        notificationRepository.save(notif);
    }

    // Notifications d'un user
    public List<Notification> getAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Non lues
    public List<Notification> getUnread(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    // Nombre de non lues
    public long countUnread(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return notificationRepository.countByUserAndReadFalse(user);
    }

    // Marquer toutes comme lues
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        notificationRepository.markAllAsRead(user);
    }

    // Marquer une seule comme lue
    public void markAsRead(Long id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        notif.setRead(true);
        notificationRepository.save(notif);
    }

    // ─── Méthodes métier ──────────────────────────────────

    // Architecte reçoit une demande de projet
    public void notifyProjectReceived(User architect, Project project) {
        create(architect,
                NotificationType.PROJECT_RECEIVED,
                "Nouvelle demande de projet",
                project.getClient().getFullName() + " vous a soumis une demande.",
                "/architect/projects/" + project.getId());
    }

    // Client : projet accepté
    public void notifyProjectAccepted(User client, Project project) {
        create(client,
                NotificationType.PROJECT_ACCEPTED,
                "Projet accepté ✅",
                "L'architecte " + project.getArchitect().getFullName() +
                        " a accepté votre projet.",
                "/client/projects/" + project.getId());
    }

    // Client : projet refusé
    public void notifyProjectCancelled(User client, Project project) {
        create(client,
                NotificationType.PROJECT_CANCELLED,
                "Projet refusé ❌",
                "L'architecte " + project.getArchitect().getFullName() +
                        " a refusé votre demande.",
                "/client/projects/" + project.getId());
    }

    // Client reçoit le plan
    public void notifyPlanSent(User client, Project project) {
        create(client,
                NotificationType.PLAN_SENT,
                "Plan de matériaux reçu 📋",
                "L'architecte a envoyé un plan pour votre projet.",
                "/client/projects/" + project.getId() + "/plan");
    }

    // Architecte : plan approuvé
    public void notifyPlanApproved(User architect, Project project) {
        create(architect,
                NotificationType.PLAN_APPROVED,
                "Plan approuvé ✅",
                project.getClient().getFullName() +
                        " a approuvé votre plan de matériaux.",
                "/architect/projects/" + project.getId());
    }

    // Architecte : plan refusé
    public void notifyPlanRejected(User architect, Project project) {
        create(architect,
                NotificationType.PLAN_REJECTED,
                "Plan refusé ❌",
                project.getClient().getFullName() +
                        " a demandé des modifications au plan.",
                "/architect/projects/" + project.getId() + "/plan");
    }

    // Client reçoit la facture
    public void notifyInvoiceSent(User client, Project project) {
        create(client,
                NotificationType.INVOICE_SENT,
                "Facture reçue 🧾",
                "Une facture est prête pour votre approbation.",
                "/client/projects/" + project.getId() + "/invoice");
    }

    // Architecte : facture approuvée
    public void notifyInvoiceApproved(User architect, Project project) {
        create(architect,
                NotificationType.INVOICE_APPROVED,
                "Facture approuvée ✅",
                project.getClient().getFullName() +
                        " a approuvé la facture. Les commandes ont été créées.",
                "/architect/projects/" + project.getId() + "/invoice");
    }

    // Fournisseur : nouvelle offre acceptée
    public void notifyOfferAccepted(User supplier, MaterialRequest request) {
        create(supplier,
                NotificationType.OFFER_ACCEPTED,
                "Offre acceptée 🎉",
                "Votre offre pour \"" + request.getTitle() +
                        "\" a été acceptée !",
                "/supplier/marketplace/" + request.getId());
    }

    // Fournisseur : nouveau bon de commande
    public void notifyOrderCreated(User supplier, PurchaseOrder order) {
        create(supplier,
                NotificationType.ORDER_CREATED,
                "Nouveau bon de commande 📦",
                "Vous avez reçu un bon de commande pour : " +
                        order.getMaterialItem().getName(),
                "/supplier/orders");
    }

    // Nouveau message
    public void notifyNewMessage(User recipient, User sender) {
        create(recipient,
                NotificationType.NEW_MESSAGE,
                "Nouveau message 💬",
                sender.getFullName() + " vous a envoyé un message.",
                "/messages");
    }

    // Architecte : facture refusée
    public void notifyInvoiceRejected(User architect, Project project) {
        create(architect,
                NotificationType.INVOICE_REJECTED,
                "Facture refusée ❌",
                project.getClient().getFullName() +
                        " a refusé la facture. Veuillez la réviser.",
                "/architect/projects/" + project.getId() + "/invoice");
    }

    // Fournisseur : offre refusée
    public void notifyOfferRejected(User supplier, MaterialRequest request) {
        create(supplier,
                NotificationType.OFFER_REJECTED,
                "Offre refusée ❌",
                "Votre offre pour \"" + request.getTitle() +
                        "\" n'a pas été retenue.",
                "/supplier/marketplace/" + request.getId());
    }

    // Client ou architecte : statut d'une commande mis à jour
    public void notifyOrderStatusUpdated(User recipient, PurchaseOrder order, String link) {
        create(recipient,
                NotificationType.ORDER_STATUS_UPDATED,
                "Commande mise à jour 📦",
                "Le statut de la commande \"" +
                        order.getMaterialItem().getName() +
                        "\" est maintenant : " + order.getStatus().name(),
                link);
    }

    // Client : projet clôturé
    public void notifyProjectCompleted(User client, Project project) {
        create(client,
                NotificationType.PROJECT_COMPLETED,
                "Projet terminé 🎉",
                "Votre projet \"" + project.getTitle() +
                        "\" a été clôturé. Vous pouvez laisser un avis.",
                "/client/projects/" + project.getId() + "/review");
    }
}