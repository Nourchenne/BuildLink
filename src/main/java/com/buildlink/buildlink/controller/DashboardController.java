package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.entity.*;
import com.buildlink.buildlink.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    private final ProjectService projectService;
    private final MarketplaceService marketplaceService;
    private final CatalogService catalogService;
    private final UserService userService;
    private final ReviewService reviewService;

    public DashboardController(ProjectService projectService,
                               MarketplaceService marketplaceService,
                               CatalogService catalogService,
                               UserService userService,
                               ReviewService reviewService) {
        this.projectService = projectService;
        this.marketplaceService = marketplaceService;
        this.catalogService = catalogService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    // ─── CLIENT ───────────────────────────────────────────
    @GetMapping("/client/dashboard")
    public String clientDashboard(Model model, Principal principal) {
        List<Project> projects = projectService.getClientProjects(principal.getName());

        long active = projects.stream()
                .filter(p -> p.getStatus() != ProjectStatus.COMPLETED
                        && p.getStatus() != ProjectStatus.CANCELLED)
                .count();

        long completed = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED)
                .count();

        long pending = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLAN_SENT)
                .count();

        // 3 projets récents
        List<Project> recent = projects.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(3)
                .toList();

        model.addAttribute("projects", recent);
        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("activeProjects", active);
        model.addAttribute("completedProjects", completed);
        model.addAttribute("pendingProjects", pending);
        return "client/dashboard";
    }

    // ─── ARCHITECTE ───────────────────────────────────────
    @GetMapping("/architect/dashboard")
    public String architectDashboard(Model model, Principal principal) {
        List<Project> projects = projectService.getArchitectProjects(principal.getName());

        long pending = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.INQUIRY)
                .count();

        long active = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLANNING
                        || p.getStatus() == ProjectStatus.PLAN_SENT
                        || p.getStatus() == ProjectStatus.PLAN_APPROVED
                        || p.getStatus() == ProjectStatus.ORDERED)
                .count();

        long completed = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED)
                .count();

        List<MaterialRequest> requests =
                marketplaceService.getArchitectRequests(principal.getName());

        long openRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.OPEN)
                .count();

        // Projets récents
        List<Project> recent = projects.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(4)
                .toList();

        model.addAttribute("projects", recent);
        model.addAttribute("pendingProjects", pending);
        model.addAttribute("activeProjects", active);
        model.addAttribute("completedProjects", completed);
        model.addAttribute("openRequests", openRequests);
        model.addAttribute("totalProjects", projects.size());
        return "architect/dashboard";
    }

    // ─── FOURNISSEUR ──────────────────────────────────────
    @GetMapping("/supplier/dashboard")
    public String supplierDashboard(Model model, Principal principal) {
        List<CatalogItem> catalog =
                catalogService.getSupplierCatalog(principal.getName());

        List<Offer> offers =
                marketplaceService.getSupplierOffers(principal.getName());

        long available = catalog.stream()
                .filter(CatalogItem::isAvailable).count();

        long acceptedOffers = offers.stream()
                .filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();

        long pendingOffers = offers.stream()
                .filter(o -> o.getStatus() == OfferStatus.PENDING).count();

        List<MaterialRequest> openRequests =
                marketplaceService.getOpenRequests();

        // 3 offres récentes
        List<Offer> recentOffers = offers.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(3)
                .toList();

        model.addAttribute("totalCatalog", catalog.size());
        model.addAttribute("availableItems", available);
        model.addAttribute("totalOffers", offers.size());
        model.addAttribute("acceptedOffers", acceptedOffers);
        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("openRequests", openRequests.size());
        model.addAttribute("recentOffers", recentOffers);
        return "supplier/dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ✅ Page "Trouver un architecte" pour les clients — fonctionnalité décrite dans BuildLink
    @GetMapping("/client/architects")
    public String searchArchitects(@RequestParam(required = false) String search,
                                   Model model) {
        List<User> architects = userService.searchArchitects(search);

        // Enrichir avec la note moyenne de chaque architecte
        List<ArchitectCard> cards = architects.stream()
                .map(a -> new ArchitectCard(
                        a,
                        reviewService.getAverageRating(a.getId()),
                        reviewService.getArchitectReviews(a.getId()).size()
                ))
                .toList();

        model.addAttribute("cards", cards);
        model.addAttribute("search", search);
        return "client/architects";
    }

    // Classe interne pour passer les données à la vue
    public static class ArchitectCard {
        public final User architect;
        public final double avgRating;
        public final int reviewCount;

        public ArchitectCard(User architect, double avgRating, int reviewCount) {
            this.architect = architect;
            this.avgRating = avgRating;
            this.reviewCount = reviewCount;
        }
    }
}