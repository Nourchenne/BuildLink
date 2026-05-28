package com.buildlink.buildlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Participants
    @ManyToOne
    @JoinColumn(name = "participant_one_id")
    private User participantOne;

    @ManyToOne
    @JoinColumn(name = "participant_two_id")
    private User participantTwo;

    // Projet lié (optionnel)
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Message> messages;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getParticipantOne() { return participantOne; }
    public void setParticipantOne(User participantOne) { this.participantOne = participantOne; }

    public User getParticipantTwo() { return participantTwo; }
    public void setParticipantTwo(User participantTwo) { this.participantTwo = participantTwo; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    // Retourne l'autre participant
    public User getOtherParticipant(String email) {
        if (participantOne.getEmail().equals(email)) return participantTwo;
        return participantOne;
    }
}