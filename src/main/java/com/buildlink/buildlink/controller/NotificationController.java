package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.Notification;
import com.buildlink.buildlink.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Page toutes les notifications
    @GetMapping
    public String allNotifications(Model model, Principal principal) {
        List<Notification> notifications =
                notificationService.getAll(principal.getName());
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    // Marquer toutes comme lues
    @PostMapping("/read-all")
    public String markAllRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return "redirect:/notifications";
    }

    // Marquer une comme lue + rediriger
    @GetMapping("/{id}/open")
    public String openNotification(@PathVariable Long id, Principal principal) {
        Notification notif = notificationService.getAll(principal.getName())
                .stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElse(null);

        notificationService.markAsRead(id);

        if (notif != null && notif.getLink() != null) {
            return "redirect:" + notif.getLink();
        }
        return "redirect:/notifications";
    }
}