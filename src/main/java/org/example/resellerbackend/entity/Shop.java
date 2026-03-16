package org.example.resellerbackend.entity;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "shops")
public class Shop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "shop_name")
    private String shopName;
    @Column(name = "shop_slug")
    private String shopSlug;
}