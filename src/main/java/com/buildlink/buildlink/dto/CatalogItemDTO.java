package com.buildlink.buildlink.dto;

import jakarta.validation.constraints.*;

public class CatalogItemDTO {

    @NotBlank(message = "Le nom de l'article est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String name;

    @NotBlank(message = "Le type est obligatoire")
    private String type;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Positive(message = "Le prix doit être positif")
    private double price;

    @Positive(message = "La quantité doit être positive")
    private double quantity;

    @NotBlank(message = "L'unité est obligatoire")
    private String unit;

    @Size(max = 300, message = "La localisation ne peut pas dépasser 300 caractères")
    private String location;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
