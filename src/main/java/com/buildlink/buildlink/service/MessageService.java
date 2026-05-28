package com.buildlink.buildlink.service;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          ProjectRepository projectRepository,
                          NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    // Toutes les conversations d'un utilisateur
    public List<Conversation> getConversations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return conversationRepository.findByUser(user);
    }

    // Récupérer ou créer une conversation entre deux utilisateurs
    @Transactional
    public Conversation getOrCreateConversation(String userEmail,
                                                Long otherUserId,
                                                Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Chercher une conversation existante
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                return conversationRepository
                        .findByProjectAndUsers(project, user, other)
                        .orElseGet(() -> createConversation(user, other, project));
            }
        }

        return conversationRepository
                .findBetween(user, other)
                .orElseGet(() -> createConversation(user, other, null));
    }

    private Conversation createConversation(User u1, User u2, Project project) {
        Conversation conv = new Conversation();
        conv.setParticipantOne(u1);
        conv.setParticipantTwo(u2);
        conv.setProject(project);
        if (project != null) {
            conv.setSubject("Projet : " + project.getTitle());
        } else {
            conv.setSubject("Discussion");
        }
        return conversationRepository.save(conv);
    }

    // Récupérer une conversation par ID
    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
    }

    // Messages d'une conversation
    public List<Message> getMessages(Long conversationId) {
        Conversation conv = getConversationById(conversationId);
        return messageRepository.findByConversationOrderBySentAtAsc(conv);
    }

    // Envoyer un message
    @Transactional
    public void sendMessage(Long conversationId, String senderEmail, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Le message ne peut pas être vide");
        }

        Conversation conv = getConversationById(conversationId);
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Message message = new Message();
        message.setConversation(conv);
        message.setSender(sender);
        message.setContent(content.trim());
        messageRepository.save(message);

        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        User recipient = conv.getOtherParticipant(senderEmail);
        notificationService.notifyNewMessage(recipient, sender);
    }

    // Marquer les messages comme lus
    @Transactional
    public void markAsRead(Long conversationId, String userEmail) {
        Conversation conv = getConversationById(conversationId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        messageRepository.markAllAsRead(conv, user);
    }
}