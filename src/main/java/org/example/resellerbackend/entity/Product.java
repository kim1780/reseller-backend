package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "cost_price")
    private BigDecimal costPrice;
    @Column(name = "min_price")
    private BigDecimal minPrice;
    private Integer stock;
    private String category;
    @Column(name = "image_url")
    private String imageUrl;
}