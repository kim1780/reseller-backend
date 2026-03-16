package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Orders {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "shop_id")
    private Long shopId;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "reseller_profit")
    private BigDecimal resellerProfit;
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}