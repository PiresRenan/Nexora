package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.ProductJpaRepository;
import com.nexora.domain.model.Product;
import com.nexora.domain.repository.ProductRepository;
import com.nexora.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    public ProductPersistenceAdapter(ProductJpaRepository jpa) { this.jpa = jpa; }

    @Override public Product save(Product p)                               { return jpa.save(ProductEntity.fromDomain(p)).toDomain(); }
    @Override public Optional<Product> findById(UUID id)                   { return jpa.findById(id).map(ProductEntity::toDomain); }
    @Override public Optional<Product> findBySku(String sku)               { return jpa.findBySku(sku).map(ProductEntity::toDomain); }
    @Override public Page<Product> findAll(Pageable pageable)              { return jpa.findAll(pageable).map(ProductEntity::toDomain); }
    @Override public Page<Product> findAllActive(Pageable pageable)        { return jpa.findAllByActiveTrue(pageable).map(ProductEntity::toDomain); }
    @Override public Page<Product> findByCategoryId(UUID cId, Pageable p)  { return jpa.findByCategoryId(cId, p).map(ProductEntity::toDomain); }
    @Override public boolean existsBySku(String sku)                       { return jpa.existsBySku(sku); }
    @Override public void deleteById(UUID id)                              { jpa.deleteById(id); }
}