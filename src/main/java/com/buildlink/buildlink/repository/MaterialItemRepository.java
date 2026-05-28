package com.buildlink.buildlink.repository;

import com.buildlink.buildlink.entity.MaterialItem;
import com.buildlink.buildlink.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialItemRepository extends JpaRepository<MaterialItem, Long> {
    List<MaterialItem> findByProject(Project project);
    void deleteByProject(Project project);
}