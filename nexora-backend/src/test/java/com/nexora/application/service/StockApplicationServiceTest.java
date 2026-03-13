package com.nexora.application.service;

import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.*;
import com.nexora.domain.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockApplicationService")
class StockApplicationServiceTest {

    @Mock StockMovementRepository stockMovementRepository;
    @Mock ProductRepository       productRepository;

    @InjectMocks StockApplicationService service;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.create("Notebook", "desc", "NB-001",
                Money.brl("999.00"), StockQuantity.of(10));
    }

    @Nested
    @DisplayName("findByProduct")
    class FindByProduct {

        @Test
        @DisplayName("Deve retornar movimentações paginadas do produto")
        void shouldReturnMovements() {
            var movement = StockMovement.entry(product.getId(), 10, 0, 10,
                    "INITIAL_STOCK", null, null);
            var pageable = PageRequest.of(0, 20);
            var page     = new PageImpl<>(List.of(movement), pageable, 1);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));
            given(stockMovementRepository.findByProductId(product.getId(), pageable))
                    .willReturn(page);

            var result = service.findByProduct(product.getId(), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().type())
                    .isEqualTo(StockMovement.Type.ENTRY);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException para produto inexistente")
        void shouldThrowForUnknownProduct() {
            var unknownId = UUID.randomUUID();
            given(productRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByProduct(unknownId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Deve retornar todas as movimentações paginadas")
        void shouldReturnAll() {
            var pageable = PageRequest.of(0, 20);
            var page     = new PageImpl<>(List.of(), pageable, 0);
            given(stockMovementRepository.findAll(pageable)).willReturn(page);

            var result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isZero();
        }
    }
}