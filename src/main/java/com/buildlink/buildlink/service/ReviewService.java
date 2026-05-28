package com.buildlink.buildlink.service;

import com.buildlink.buildlink.dto.ReviewDTO;
import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PurchaseOrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProjectRepository projectRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         PurchaseOrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    // Client laisse un avis — uniquement sur un projet COMPLETED
    @Transactional
    public void leaveReview(Long projectId, ReviewDTO dto, String clientEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        if (!project.getClient().getEmail().equals(clientEmail)) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new RuntimeException("Le projet doit être terminé pour laisser un avis");
        }

        if (reviewRepository.existsByProject(project)) {
            throw new RuntimeException("Vous avez déjà laissé un avis pour ce projet");
        }

        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("La note doit être entre 1 et 5");
        }

        Review review = new Review();
        review.setProject(project);
        review.setClient(project.getClient());
        review.setArchitect(project.getArchitect());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        reviewRepository.save(review);
        log.info("Avis ({} étoiles) laissé par {} pour le projet {}", dto.getRating(), clientEmail, projectId);
    }

    // Avis d'un architecte
    public List<Review> getArchitectReviews(Long architectId) {
        User architect = userRepository.findById(architectId)
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));
        return reviewRepository.findByArchitect(architect);
    }

    // Note moyenne d'un architecte
    public double getAverageRating(Long architectId) {
        User architect = userRepository.findById(architectId)
                .orElseThrow(() -> new RuntimeException("Architecte non trouvé"));
        Double avg = reviewRepository.getAverageRatingForArchitect(architect);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    // Avis d'un projet
    public Review getByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return reviewRepository.findByProject(project).orElse(null);
    }

    /**
     * Clôture formelle du projet par l'architecte.
     * Selon le flux BuildLink :
     *  - Toutes les commandes livrées → auto COMPLETED (InvoiceService)
     *  - Architecte clique "Clôturer" → ce service notifie le client de laisser un avis
     * Accepte les statuts ORDERED (si pas encore auto-complété) ou COMPLETED.
     */
    @Transactional
    public void completeProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        if (project.getStatus() != ProjectStatus.ORDERED
                && project.getStatus() != ProjectStatus.COMPLETED) {
            throw new RuntimeException(
                    "Le projet doit être en cours de livraison (ORDERED) ou déjà complété pour être clôturé formellement");
        }

        List<PurchaseOrder> orders = orderRepository.findByProject(project);

        // ✅ Si des commandes existent, elles doivent toutes être livrées
        if (!orders.isEmpty()) {
            boolean allDelivered = orders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.DELIVERED);
            if (!allDelivered) {
                throw new RuntimeException(
                        "Toutes les commandes doivent être livrées avant de clôturer le projet");
            }
        }
        // ✅ Si pas de commandes (cas sans marketplace), on peut clôturer directement depuis ORDERED

        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project);

        log.info("Projet {} formellement clôturé par l'architecte {}", projectId, project.getArchitect().getEmail());

        // ✅ Notifier le client qu'il peut laisser un avis (flux principal BuildLink)
        notificationService.notifyProjectCompleted(project.getClient(), project);
    }
}