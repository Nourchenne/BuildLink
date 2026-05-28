package com.buildlink.buildlink.service;

import com.buildlink.buildlink.dto.*;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketplaceService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceService.class);

    private final MaterialRequestRepository requestRepository;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public MarketplaceService(MaterialRequestRepository requestRepository,
                              OfferRepository offerRepository,
                              UserRepository userRepository,
                              ProjectRepository projectRepository,
                              NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    public void createRequest(MaterialRequestDTO dto, String architectEmail) {
        User architect = userRepository.findByEmail(architectEmail)
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));

        // ✅ Vérification du rôle
        if (architect.getRole() != Role.ARCHITECT) {
            throw new RuntimeException("Seul un architecte peut publier une demande de matériaux");
        }

        MaterialRequest request = new MaterialRequest();
        request.setTitle(dto.getTitle());
        request.setMaterialType(dto.getMaterialType());
        request.setDescription(dto.getDescription());
        request.setQuantity(dto.getQuantity());
        request.setUnit(dto.getUnit());
        request.setLocation(dto.getLocation());
        request.setArchitect(architect);

        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
            // ✅ Vérification que le projet appartient bien à cet architecte
            if (!project.getArchitect().getEmail().equals(architectEmail)) {
                throw new RuntimeException("Ce projet ne vous appartient pas");
            }
            request.setProject(project);
        }

        requestRepository.save(request);
        log.info("Demande de matériaux '{}' publiée par {}", dto.getTitle(), architectEmail);
    }

    public List<MaterialRequest> getArchitectRequests(String email) {
        User architect = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));
        return requestRepository.findByArchitect(architect);
    }

    public MaterialRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
    }

    public List<Offer> getOffersForRequest(Long requestId) {
        MaterialRequest request = getRequestById(requestId);
        return offerRepository.findByRequest(request);
    }

    @Transactional
    public void acceptOffer(Long offerId) {
        Offer accepted = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

        MaterialRequest request = accepted.getRequest();

        List<Offer> allOffers = offerRepository.findByRequest(request);
        for (Offer offer : allOffers) {
            if (!offer.getId().equals(offerId)) {
                offer.setStatus(OfferStatus.REJECTED);
                offerRepository.save(offer);
                notificationService.notifyOfferRejected(offer.getSupplier(), request);
            }
        }

        accepted.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(accepted);

        request.setStatus(RequestStatus.CLOSED);
        requestRepository.save(request);

        notificationService.notifyOfferAccepted(accepted.getSupplier(), request);
        log.info("Offre {} acceptée pour la demande {}", offerId, request.getId());
    }

    public void closeRequest(Long requestId) {
        MaterialRequest request = getRequestById(requestId);
        request.setStatus(RequestStatus.CLOSED);
        requestRepository.save(request);
        log.info("Demande {} fermée manuellement", requestId);
    }

    public List<MaterialRequest> getOpenRequests() {
        return requestRepository.findByStatus(RequestStatus.OPEN);
    }

    public void submitOffer(Long requestId, OfferDTO dto, String supplierEmail) {
        User supplier = userRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));

        // ✅ Vérification que le fournisseur est bien un SUPPLIER
        if (supplier.getRole() != Role.SUPPLIER) {
            throw new RuntimeException("Seul un fournisseur peut soumettre une offre");
        }

        MaterialRequest request = getRequestById(requestId);

        if (request.getStatus() != RequestStatus.OPEN) {
            throw new RuntimeException("Cette demande n'accepte plus d'offres");
        }

        if (offerRepository.existsByRequestAndSupplier(request, supplier)) {
            throw new RuntimeException("Vous avez déjà soumis une offre pour cette demande");
        }

        Offer offer = new Offer();
        offer.setUnitPrice(dto.getUnitPrice());
        offer.setQuantity(dto.getQuantity());
        offer.setUnit(dto.getUnit());
        offer.setMessage(dto.getMessage());
        offer.setDeliveryTime(dto.getDeliveryTime());
        offer.setSupplier(supplier);
        offer.setRequest(request);

        offerRepository.save(offer);
        log.info("Offre soumise par {} pour la demande {}", supplierEmail, requestId);
    }

    public List<Offer> getSupplierOffers(String email) {
        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));
        return offerRepository.findBySupplier(supplier);
    }

    public Offer getSupplierOfferForRequest(Long requestId, String supplierEmail) {
        User supplier = userRepository.findByEmail(supplierEmail).orElse(null);
        if (supplier == null) return null;
        MaterialRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null) return null;
        return offerRepository.findByRequestAndSupplier(request, supplier).orElse(null);
    }
}