//package com.nexora.application.service;
//
//import com.nexora.application.dto.order.CreateOrderRequest;
//import com.nexora.application.dto.order.OrderItemRequest;
//import com.nexora.domain.exception.BusinessRuleException;
//import com.nexora.domain.exception.ResourceNotFoundException;
//import com.nexora.domain.model.*;
//import com.nexora.domain.port.EventPublisher;
//import com.nexora.domain.repository.*;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("OrderApplicationService")
//class OrderApplicationServiceTest {
//
//    @Mock OrderRepository         orderRepository;
//    @Mock ProductRepository       productRepository;
//    @Mock StockMovementRepository stockMovementRepository;
//    @Mock UserRepository          userRepository;
//    @Mock EventPublisher          eventPublisher;   // Phase 3: necessário no construtor
//
//    @InjectMocks OrderApplicationService service;
//
//    private User    customer;
//    private Product product;
//
//    @BeforeEach
//    void setUp() {
//        customer = User.create("Customer", "customer@test.com", "hashed", UserRole.CUSTOMER);
//        product  = Product.create("Notebook", "desc", "NB-001",
//                Money.brl("1000.00"), StockQuantity.of(10));
//    }
//
//    // ─── createOrder ──────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("Should create order with valid items")
//    void shouldCreateOrder() {
//        given(userRepository.findById(customer.getId())).willReturn(Optional.of(customer));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//
//        var result = service.createOrder(customer.getId(), new CreateOrderRequest(
//                List.of(new OrderItemRequest(product.getId(), 2)), "Test notes"
//        ));
//
//        assertThat(result.items()).hasSize(1);
//        assertThat(result.items().getFirst().quantity()).isEqualTo(2);
//        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
//        then(eventPublisher).shouldHaveNoInteractions(); // evento só na confirmação
//    }
//
//    @Test
//    @DisplayName("Should throw ResourceNotFoundException when customer not found")
//    void shouldThrowWhenCustomerNotFound() {
//        given(userRepository.findById(any())).willReturn(Optional.empty());
//
//        assertThatThrownBy(() -> service.createOrder(UUID.randomUUID(), new CreateOrderRequest(
//                List.of(new OrderItemRequest(UUID.randomUUID(), 1)), null
//        ))).isInstanceOf(ResourceNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("Should throw BusinessRuleException on insufficient stock")
//    void shouldThrowOnInsufficientStock() {
//        var lowStockProduct = Product.create("Scarce", "desc", "SC-001",
//                Money.brl("50.00"), StockQuantity.of(2));
//
//        given(userRepository.findById(customer.getId())).willReturn(Optional.of(customer));
//        given(productRepository.findById(lowStockProduct.getId())).willReturn(Optional.of(lowStockProduct));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//
//        // Cria pedido com 2 unidades (no limite)
//        var order = service.createOrder(customer.getId(), new CreateOrderRequest(
//                List.of(new OrderItemRequest(lowStockProduct.getId(), 2)), null
//        ));
//        assertThat(order.items()).hasSize(1);
//
//        // Simula tentativa de confirmar com produto sem estoque
//        var confirmedOrder = Order.create(customer.getId(), null);
//        confirmedOrder.addItem(lowStockProduct, 2);
//        var emptyStock = Product.create("Empty", "desc", "EM-001",
//                Money.brl("10.00"), StockQuantity.of(0));
//
//        given(orderRepository.findById(any())).willReturn(Optional.of(confirmedOrder));
//        given(productRepository.findById(lowStockProduct.getId()))
//                .willReturn(Optional.of(emptyStock));  // estoque zerado
//
//        assertThatThrownBy(() -> service.confirmOrder(confirmedOrder.getId(), UUID.randomUUID()))
//                .isInstanceOf(BusinessRuleException.class)
//                .hasMessageContaining("Insufficient stock");
//    }
//
//    // ─── confirmOrder ─────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("Should confirm order, decrement stock, and publish event")
//    void shouldConfirmAndDecrementStock() {
//        var order = Order.create(customer.getId(), null);
//        order.addItem(product, 3);
//
//        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        willDoNothing().given(eventPublisher).publish(any());
//
//        var result = service.confirmOrder(order.getId(), UUID.randomUUID());
//
//        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
//        // Estoque decrementado: 10 - 3 = 7
//        then(productRepository).should().save(argThat(p -> p.getStock().value() == 7));
//        then(stockMovementRepository).should().save(argThat(m -> m.getType() == StockMovement.Type.EXIT));
//        then(eventPublisher).should().publish(any());  // OrderConfirmedEvent publicado
//    }
//
//    // ─── cancelOrder ──────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("Should restore stock when cancelling a CONFIRMED order")
//    void shouldRestoreStockOnCancelConfirmed() {
//        var order = Order.create(customer.getId(), null);
//        order.addItem(product, 3);
//        order.confirm();   // CONFIRMED — estoque está comprometido
//
//        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        willDoNothing().given(eventPublisher).publish(any());
//
//        service.cancelOrder(order.getId(), customer.getId(), "Changed mind");
//
//        // Estoque devolvido: 10 + 3 = 13
//        then(productRepository).should().save(argThat(p -> p.getStock().value() == 13));
//        then(stockMovementRepository).should().save(argThat(m -> m.getType() == StockMovement.Type.ENTRY));
//        then(eventPublisher).should().publish(any());  // OrderCancelledEvent publicado
//    }
//
//    @Test
//    @DisplayName("Should NOT restore stock when cancelling a PENDING order")
//    void shouldNotRestoreStockOnCancelPending() {
//        var order = Order.create(customer.getId(), null);
//        order.addItem(product, 3);
//        // Status PENDING — estoque NÃO foi comprometido
//
//        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        willDoNothing().given(eventPublisher).publish(any());
//
//        service.cancelOrder(order.getId(), customer.getId(), "Cancelled before confirmation");
//
//        // Nenhuma interação com productRepository nem stockMovementRepository
//        then(productRepository).shouldHaveNoInteractions();
//        then(stockMovementRepository).shouldHaveNoInteractions();
//    }
//}