package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    public MessageController(MessageService messageService,
                             UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    // Liste des conversations
    @GetMapping
    public String inbox(Model model, Principal principal) {
        List<Conversation> conversations =
                messageService.getConversations(principal.getName());
        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUserEmail", principal.getName());
        return "messages/inbox";
    }

    // Ouvrir une conversation
    @GetMapping("/{id}")
    public String conversation(@PathVariable Long id,
                               Model model,
                               Principal principal) {
        Conversation conv = messageService.getConversationById(id);
        List<Message> messages = messageService.getMessages(id);

        // Marquer comme lus
        messageService.markAsRead(id, principal.getName());

        model.addAttribute("conversation", conv);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUserEmail", principal.getName());
        return "messages/conversation";
    }

    // Envoyer un message
    @PostMapping("/{id}/send")
    public String sendMessage(@PathVariable Long id,
                              @RequestParam String content,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            messageService.sendMessage(id, principal.getName(), content);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/messages/" + id;
    }

    // Démarrer une conversation depuis un projet
    @GetMapping("/start")
    public String startConversation(@RequestParam Long userId,
                                    @RequestParam(required = false) Long projectId,
                                    Principal principal) {
        Conversation conv = messageService.getOrCreateConversation(
                principal.getName(), userId, projectId);
        return "redirect:/messages/" + conv.getId();
    }
}