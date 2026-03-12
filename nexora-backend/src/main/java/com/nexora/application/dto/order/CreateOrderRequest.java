package com.nexora.application.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 300)
        String notes
) {}