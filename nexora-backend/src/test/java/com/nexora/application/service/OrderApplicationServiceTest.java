//package com.nexora.application.service;
//
//import com.nexora.application.dto.order.CreateOrderRequest;
//import com.nexora.application.dto.order.OrderItemRequest;
//import com.nexora.domain.exception.BusinessRuleException;
//import com.nexora.domain.exception.ResourceNotFoundException;
//import com.nexora.domain.model.*;
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
//
//    @InjectMocks OrderApplicationService service;
//
//    private User     customer;
//    private Product  product;
//
//    @BeforeEach
//    void setUp() {
//        customer = User.create("Customer", "customer@test.com",
//                "hashed", UserRole.CUSTOMER);
//        product = Product.create("Notebook", "desc", "NB-001",
//                Money.brl("1000.00"), StockQuantity.of(10));
//    }
//
//    @Test
//    @DisplayName("Should create order with valid items")
//    void shouldCreateOrder() {
//        given(userRepository.findById(customer.getId())).willReturn(Optional.of(customer));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//
//        var request = new CreateOrderRequest(
//                List.of(new OrderItemRequest(product.getId(), 2)), "Test notes"
//        );
//
//        var result = service.createOrder(customer.getId(), request);
//
//        assertThat(result.items()).hasSize(1);
//        assertThat(result.items().getFirst().quantity()).isEqualTo(2);
//        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
//    }
//
//    @Test
//    @DisplayName("Should throw when customer not found")
//    void shouldThrowWhenCustomerNotFound() {
//        given(userRepository.findById(any())).willReturn(Optional.empty());
//        var request = new CreateOrderRequest(List.of(new OrderItemRequest(UUID.randomUUID(), 1)), null);
//
//        assertThatThrownBy(() -> service.createOrder(UUID.randomUUID(), request))
//                .isInstanceOf(ResourceNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("Should confirm order and decrement stock")
//    void shouldConfirmAndDecrementStock() {
//        var order = Order.create(customer.getId(), null);
//        order.addItem(product, 3);
//
//        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//
//        var result = service.confirmOrder(order.getId(), UUID.randomUUID());
//
//        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
//        // Verifica que estoque foi decrementado
//        then(productRepository).should().save(argThat(p -> p.getStock().value() == 7));
//        then(stockMovementRepository).should().save(argThat(m -> m.getType() == StockMovement.Type.EXIT));
//    }
//
//    @Test
//    @DisplayName("Should restore stock when cancelling confirmed order")
//    void shouldRestoreStockOnCancel() {
//        var order = Order.create(customer.getId(), null);
//        order.addItem(product, 3);
//        order.confirm(); // muda status para CONFIRMED
//
//        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
//        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
//        given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//        given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
//
//        service.cancelOrder(order.getId(), customer.getId(), "Changed mind");
//
//        // Estoque deve ser devolvido
//        then(productRepository).should().save(argThat(p -> p.getStock().value() == 13));
//        then(stockMovementRepository).should().save(argThat(m -> m.getType() == StockMovement.Type.ENTRY));
//    }
//}