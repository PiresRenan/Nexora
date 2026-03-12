package com.nexora.adapter.output.persistence.jpa;

import com.nexora.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    Optional<ProductEntity>  findBySku(String sku);
    Page<ProductEntity>      findAllByActiveTrue(Pageable pageable);
    Page<ProductEntity>      findByCategoryId(UUID categoryId, Pageable pageable);
    boolean                  existsBySku(String sku);
}