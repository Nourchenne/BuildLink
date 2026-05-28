package com.buildlink.buildlink.entity;

public enum NotificationType {
    PROJECT_RECEIVED,      // Architecte reçoit une demande
    PROJECT_ACCEPTED,      // Client : projet accepté
    PROJECT_CANCELLED,     // Client : projet refusé
    PLAN_SENT,             // Client : plan reçu
    PLAN_APPROVED,         // Architecte : plan approuvé
    PLAN_REJECTED,         // Architecte : plan refusé
    INVOICE_SENT,          // Client : facture reçue
    INVOICE_APPROVED,      // Architecte : facture approuvée
    INVOICE_REJECTED,      // Architecte : facture refusée
    ORDER_CREATED,         // Fournisseur : nouveau bon de commande
    ORDER_STATUS_UPDATED,  // Client/Architecte : statut commande mis à jour
    OFFER_ACCEPTED,        // Fournisseur : offre acceptée
    OFFER_REJECTED,        // Fournisseur : offre refusée
    NEW_MESSAGE,           // Nouveau message reçu
    PROJECT_COMPLETED      // Client/Architecte : projet terminé
}