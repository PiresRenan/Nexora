package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.Money;
import com.nexora.domain.model.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class OrderItemEmbeddable {

    @Column(name = "product_id",   nullable = false) private UUID       productId;
    @Column(name = "product_name", nullable = false, length = 100) private String productName;
    @Column(name = "product_sku",  nullable = false, length = 20)  private String productSku;
    @Column(name = "unit_price",   nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "currency",     nullable = false, length = 3)   private String currency;
    @Column(name = "quantity",     nullable = false) private int quantity;

    protected OrderItemEmbeddable() {}

    public static OrderItemEmbeddable fromDomain(OrderItem item) {
        var e = new OrderItemEmbeddable();
        e.productId = item.productId(); e.productName = item.productName();
        e.productSku = item.productSku(); e.unitPrice = item.unitPrice().amount();
        e.currency = item.unitPrice().currency(); e.quantity = item.quantity();
        return e;
    }

    public OrderItem toDomain() {
        return new OrderItem(productId, productName, productSku,
                Money.of(unitPrice, currency), quantity);
    }
}