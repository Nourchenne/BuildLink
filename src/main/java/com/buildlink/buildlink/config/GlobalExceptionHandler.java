package com.buildlink.buildlink.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ Accès refusé (403)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Accès refusé : {}", ex.getMessage());
        model.addAttribute("errorTitle", "Accès refusé");
        model.addAttribute("errorMessage", "Vous n'avez pas les droits pour accéder à cette ressource.");
        model.addAttribute("statusCode", 403);
        return "error/error";
    }

    // ✅ Ressource non trouvée (RuntimeException générique)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException ex,
                                         HttpServletRequest request,
                                         Model model) {
        log.error("Erreur inattendue sur [{}] : {}", request.getRequestURI(), ex.getMessage(), ex);
        model.addAttribute("errorTitle", "Une erreur est survenue");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("statusCode", 500);
        return "error/error";
    }

    // ✅ Exception générale (catch-all)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         Model model) {
        log.error("Exception inattendue sur [{}] : {}", request.getRequestURI(), ex.getMessage(), ex);
        model.addAttribute("errorTitle", "Erreur interne du serveur");
        model.addAttribute("errorMessage", "Une erreur interne est survenue. Veuillez réessayer.");
        model.addAttribute("statusCode", 500);
        return "error/error";
    }
}

