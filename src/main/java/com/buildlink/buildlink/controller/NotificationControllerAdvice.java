package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.User;
import com.buildlink.buildlink.service.NotificationService;
import com.buildlink.buildlink.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationControllerAdvice {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationControllerAdvice(NotificationService notificationService,
                                        UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                long count = notificationService.countUnread(authentication.getName());
                model.addAttribute("unreadNotifications", count);

                // Injection du fullName et de l'entité User dans tous les modèles
                User user = userService.findByEmail(authentication.getName());
                model.addAttribute("currentUser", user);
                model.addAttribute("currentUserFullName", user.getFullName());
                model.addAttribute("currentUserEmail", user.getEmail());
            } catch (Exception e) {
                model.addAttribute("unreadNotifications", 0L);
            }
        }
    }
}