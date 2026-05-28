package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByProject(Project project);

    List<Review> findByArchitect(User architect);

    boolean existsByProject(Project project);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.architect = :architect")
    Double getAverageRatingForArchitect(@Param("architect") User architect);
}