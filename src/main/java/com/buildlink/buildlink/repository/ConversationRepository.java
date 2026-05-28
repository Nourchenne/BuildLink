package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Toutes les conversations d'un utilisateur
    @Query("SELECT c FROM Conversation c WHERE " +
            "c.participantOne = :user OR c.participantTwo = :user " +
            "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByUser(@Param("user") User user);

    // Conversation existante entre deux utilisateurs
    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.participantOne = :u1 AND c.participantTwo = :u2) OR " +
            "(c.participantOne = :u2 AND c.participantTwo = :u1)")
    Optional<Conversation> findBetween(@Param("u1") User u1,
                                       @Param("u2") User u2);

    // Conversation liée à un projet entre deux utilisateurs
    @Query("SELECT c FROM Conversation c WHERE c.project = :project AND " +
            "((c.participantOne = :u1 AND c.participantTwo = :u2) OR " +
            "(c.participantOne = :u2 AND c.participantTwo = :u1))")
    Optional<Conversation> findByProjectAndUsers(@Param("project") Project project,
                                                 @Param("u1") User u1,
                                                 @Param("u2") User u2);
}