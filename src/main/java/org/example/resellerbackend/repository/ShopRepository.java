package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByShopSlug(String shopSlug);

    // ✅ เพิ่ม method นี้ — ดึง Shop ด้วย userId (ใช้ตอน login)
    Optional<Shop> findByUserId(Long userId);
}