package com.nexora.domain.repository;

import com.nexora.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product               save(Product product);
    Optional<Product>     findById(UUID id);
    Optional<Product>     findBySku(String sku);
    Page<Product>         findAll(Pageable pageable);
    Page<Product>         findAllActive(Pageable pageable);
    Page<Product>         findByCategoryId(UUID categoryId, Pageable pageable);
    boolean               existsBySku(String sku);
    void                  deleteById(UUID id);
}