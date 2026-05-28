package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.CatalogItem;
import com.buildlink.buildlink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CatalogRepository extends JpaRepository<CatalogItem, Long> {

    // Catalogue d'un fournisseur
    List<CatalogItem> findBySupplier(User supplier);

    // Tous les disponibles
    List<CatalogItem> findByAvailableTrue();

    // Recherche par nom ou type
    @Query("SELECT c FROM CatalogItem c WHERE c.available = true AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.type) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<CatalogItem> searchByKeyword(@Param("keyword") String keyword);
}