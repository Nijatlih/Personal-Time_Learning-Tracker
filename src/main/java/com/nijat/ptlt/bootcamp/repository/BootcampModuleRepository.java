package com.nijat.ptlt.bootcamp.repository;

import com.nijat.ptlt.bootcamp.entity.BootcampModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Bootcamp模块Repository
 */
@Repository
public interface BootcampModuleRepository extends JpaRepository<BootcampModule, UUID> {
    /**
     * 按orderIndex排序查找所有模块
     */
    List<BootcampModule> findAllByOrderByOrderIndexAsc();
}
