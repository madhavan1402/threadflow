package com.threadflow.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_variants")
public class OrderVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;
    @Column(nullable = false) private String color;
    @Column(nullable = false) private String size;
    @Column(nullable = false) private Integer quantity;

    protected OrderVariant() {}
    public OrderVariant(ProductionOrder order, String color, String size, Integer quantity) {
        this.order = order; this.color = color; this.size = size; this.quantity = quantity;
    }
    public Long getId() { return id; }
    public String getColor() { return color; }
    public String getSize() { return size; }
    public Integer getQuantity() { return quantity; }
}
