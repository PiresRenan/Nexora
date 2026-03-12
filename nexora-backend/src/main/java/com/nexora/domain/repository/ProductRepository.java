package com.nexora.domain.repository;

import com.nexora.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port — interface que o domínio exige para persistência de produtos.
 * A implementação real fica na camada de infraestrutura (adapter/output).
 * O domínio não sabe nada de JPA, SQL ou qualquer framework.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    List<Product> findAll();

    List<Product> findAllActive();

    boolean existsBySku(String sku);

    void deleteById(UUID id);
}