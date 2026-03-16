package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @Column(name = "user_id")
    private Long userId;
    private BigDecimal balance;
}