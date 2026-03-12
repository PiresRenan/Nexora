package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.ProductJpaRepository;
import com.nexora.domain.model.Product;
import com.nexora.domain.repository.ProductRepository;
import com.nexora.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída — implementa o output port ProductRepository do domínio
 * usando Spring Data JPA.
 * <p>
 * Esta classe é o único ponto de contato entre a lógica de negócio e o banco de dados.
 * Melhoria: separação explícita da conversão domain ↔ entity nos próprios métodos,
 * sem classe mapper separada — mantém coesão sem over-engineering nesta fase.
 */
@Component
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductPersistenceAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        var entity = ProductEntity.fromDomain(product);
        var saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(ProductEntity::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return jpaRepository.findBySku(sku).map(ProductEntity::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream()
                .map(ProductEntity::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAllActive() {
        return jpaRepository.findAllByActiveTrue().stream()
                .map(ProductEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepository.existsBySku(sku);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}