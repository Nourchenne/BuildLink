package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByProject(Project project);
    List<PurchaseOrder> findBySupplier(User supplier);
    List<PurchaseOrder> findByProjectAndSupplier(Project project, User supplier);
    List<PurchaseOrder> findByProjectIn(List<Project> projects);
    List<PurchaseOrder> findByStatus(OrderStatus status);
}