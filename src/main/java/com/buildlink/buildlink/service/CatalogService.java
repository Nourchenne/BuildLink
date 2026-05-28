package com.buildlink.buildlink.service;

import com.buildlink.buildlink.dto.CatalogItemDTO;
import com.buildlink.buildlink.entity.CatalogItem;
import com.buildlink.buildlink.entity.Role;
import com.buildlink.buildlink.entity.User;
import com.buildlink.buildlink.repository.CatalogRepository;
import com.buildlink.buildlink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CatalogRepository catalogRepository;
    private final UserRepository userRepository;

    public CatalogService(CatalogRepository catalogRepository,
                          UserRepository userRepository) {
        this.catalogRepository = catalogRepository;
        this.userRepository = userRepository;
    }

    // Tous les articles disponibles
    public List<CatalogItem> getAllAvailable() {
        return catalogRepository.findByAvailableTrue();
    }

    // Recherche
    public List<CatalogItem> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllAvailable();
        }
        return catalogRepository.searchByKeyword(keyword);
    }

    // Catalogue d'un fournisseur
    public List<CatalogItem> getSupplierCatalog(String email) {
        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));
        return catalogRepository.findBySupplier(supplier);
    }

    // Ajouter un article au catalogue
    public void addItem(CatalogItemDTO dto, String supplierEmail) {
        User supplier = userRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));

        // ✅ Vérification du rôle SUPPLIER
        if (supplier.getRole() != Role.SUPPLIER) {
            throw new RuntimeException("Seul un fournisseur peut publier des articles dans le catalogue");
        }

        CatalogItem item = new CatalogItem();
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setQuantity(dto.getQuantity());
        item.setUnit(dto.getUnit());
        item.setLocation(dto.getLocation());
        item.setSupplier(supplier);
        item.setAvailable(true);

        catalogRepository.save(item);
        log.info("Article '{}' ajouté au catalogue par {}", dto.getName(), supplierEmail);
    }

    // Modifier disponibilité (avec vérification propriété)
    public void toggleAvailability(Long itemId, String supplierEmail) {
        CatalogItem item = catalogRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));
        if (!item.getSupplier().getEmail().equals(supplierEmail)) {
            throw new RuntimeException("Accès non autorisé");
        }
        item.setAvailable(!item.isAvailable());
        catalogRepository.save(item);
    }

    // Supprimer (avec vérification propriété)
    public void deleteItem(Long itemId, String supplierEmail) {
        CatalogItem item = catalogRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));
        if (!item.getSupplier().getEmail().equals(supplierEmail)) {
            throw new RuntimeException("Accès non autorisé");
        }
        catalogRepository.deleteById(itemId);
    }

    // Récupérer par ID
    public CatalogItem getById(Long id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));
    }
}