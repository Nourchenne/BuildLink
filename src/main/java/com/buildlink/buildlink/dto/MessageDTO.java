package com.buildlink.buildlink.dto;

import jakarta.validation.constraints.*;

public class MessageDTO {

    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
