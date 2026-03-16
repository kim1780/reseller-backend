package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wallet_logs")
public class WalletLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "order_id")
    private Long orderId;
    private BigDecimal amount;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}