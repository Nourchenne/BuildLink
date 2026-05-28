package com.buildlink.buildlink.dto;

import jakarta.validation.constraints.*;

public class OfferDTO {

    @Positive(message = "Le prix unitaire doit être positif")
    private double unitPrice;

    @Positive(message = "La quantité doit être positive")
    private double quantity;

    @NotBlank(message = "L'unité est obligatoire")
    private String unit;

    @Size(max = 1000, message = "Le message ne peut pas dépasser 1000 caractères")
    private String message;

    @NotBlank(message = "Le délai de livraison est obligatoire")
    @Size(max = 100, message = "Le délai ne peut pas dépasser 100 caractères")
    private String deliveryTime;

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }
}
