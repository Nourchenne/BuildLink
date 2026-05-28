package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.Role;
import com.buildlink.buildlink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);

    // ✅ Recherche d'architectes par nom (pour la page de recherche client)
    @Query("SELECT u FROM User u WHERE u.role = 'ARCHITECT' AND u.enabled = true " +
           "AND (:name IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<User> searchArchitects(@Param("name") String name);
}