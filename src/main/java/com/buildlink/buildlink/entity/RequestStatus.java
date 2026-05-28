package com.buildlink.buildlink.entity;

public enum RequestStatus {
    OPEN,       // Demande ouverte, reçoit des offres
    CLOSED,     // Architecte a choisi une offre
    CANCELLED   // Annulée
}