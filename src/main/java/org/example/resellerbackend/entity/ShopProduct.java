package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "shop_products")
public class ShopProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "shop_id")
    private Long shopId;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    private String status = "Active";
}