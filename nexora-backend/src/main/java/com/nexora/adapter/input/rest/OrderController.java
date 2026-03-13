        package com.nexora.adapter.input.rest;

import com.nexora.application.dto.order.CreateOrderRequest;
import com.nexora.application.dto.order.OrderResponse;
import com.nexora.application.usecase.OrderUseCase;
import com.nexora.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderUseCase orderUseCase;
    public OrderController(OrderUseCase orderUseCase) { this.orderUseCase = orderUseCase; }

    @PostMapping
    @Operation(summary = "Create a new order (customer)")
    public ResponseEntity<OrderResponse> createOrder(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orderUseCase.createOrder(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
    }

    @GetMapping("/my")
    @Operation(summary = "List my orders (paginated)")
    public Page<OrderResponse> myOrders(
            @CurrentUser UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return orderUseCase.findMyOrders(userId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable UUID id,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(orderUseCase.findById(id, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "List all orders — staff only")
    public Page<OrderResponse> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return orderUseCase.findAll(pageable);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Confirm order — staff only")
    public ResponseEntity<OrderResponse> confirm(@PathVariable UUID id, @CurrentUser UUID userId) {
        return ResponseEntity.ok(orderUseCase.confirmOrder(id, userId));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mark order as shipped")
    public ResponseEntity<OrderResponse> ship(@PathVariable UUID id, @CurrentUser UUID userId) {
        return ResponseEntity.ok(orderUseCase.shipOrder(id, userId));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mark order as delivered")
    public ResponseEntity<OrderResponse> deliver(@PathVariable UUID id, @CurrentUser UUID userId) {
        return ResponseEntity.ok(orderUseCase.deliverOrder(id, userId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order (customer cancels own; staff cancels any)")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable UUID id,
            @CurrentUser UUID userId,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(orderUseCase.cancelOrder(id, userId, reason));
    }
}