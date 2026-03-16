package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id")
    private Long orderId;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "cost_price")
    private BigDecimal costPrice;
    @Column(name = "selling_price")
    private BigDecimal sellingPrice;
    private Integer quantity;
}