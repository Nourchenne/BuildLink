package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    // Compter les messages non lus d'un utilisateur
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "(m.conversation.participantOne = :user OR " +
            "m.conversation.participantTwo = :user) " +
            "AND m.sender != :user AND m.read = false")
    long countUnreadForUser(@Param("user") User user);

    // Marquer tous les messages d'une conversation comme lus
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.read = true WHERE " +
            "m.conversation = :conv AND m.sender != :user")
    void markAllAsRead(@Param("conv") Conversation conv,
                       @Param("user") User user);
}