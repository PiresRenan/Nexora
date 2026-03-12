package com.nexora.adapter.output.persistence.jpa;

import com.nexora.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA — detalhe de implementação da infraestrutura.
 * Nunca deve ser usada diretamente fora da camada de adaptadores.
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySku(String sku);

    List<ProductEntity> findAllByActiveTrue();

    boolean existsBySku(String sku);
}