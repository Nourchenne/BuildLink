package com.buildlink.buildlink.dto;

import jakarta.validation.constraints.*;

public class ProjectDTO {

    @NotBlank(message = "Le titre du projet est obligatoire")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotBlank(message = "La localisation est obligatoire")
    @Size(max = 300, message = "La localisation ne peut pas dépasser 300 caractères")
    private String location;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le budget doit être positif")
    private Double budget;

    @NotNull(message = "L'architecte est obligatoire")
    private Long architectId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public Long getArchitectId() { return architectId; }
    public void setArchitectId(Long architectId) { this.architectId = architectId; }
}
