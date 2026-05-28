package com.buildlink.buildlink.entity;

public enum OrderStatus {
    PENDING,     // Bon de commande créé, en attente d'acceptation fournisseur
    CONFIRMED,   // Fournisseur a confirmé et accepté
    PREPARING,   // En préparation
    SHIPPED,     // Expédié / En cours de livraison
    DELIVERED,   // Livré
    REJECTED     // Fournisseur a refusé la commande
}