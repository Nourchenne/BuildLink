package com.buildlink.buildlink.dto;

import jakarta.validation.constraints.*;

public class MaterialItemDTO {

    @NotBlank(message = "Le nom du matériau est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String name;

    @NotBlank(message = "Le type est obligatoire")
    private String type;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Positive(message = "La quantité doit être positive")
    private double quantity;

    @NotBlank(message = "L'unité est obligatoire")
    private String unit;

    @PositiveOrZero(message = "Le prix estimé ne peut pas être négatif")
    private double estimatedPrice;

    private Long supplierId;  // ID du fournisseur assigné à ce matériau

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
}
