package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByRequest(MaterialRequest request);

    List<Offer> findBySupplier(User supplier);

    List<Offer> findBySupplierAndStatus(User supplier, OfferStatus status);

    List<Offer> findByRequest_ProjectAndStatus(Project project, OfferStatus status);

    boolean existsByRequestAndSupplier(MaterialRequest request, User supplier);

    Optional<Offer> findByRequestAndSupplier(MaterialRequest request, User supplier);
}