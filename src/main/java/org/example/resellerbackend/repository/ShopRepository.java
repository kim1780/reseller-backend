package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // อย่าลืม Import ตัวนี้นะสัส! สำคัญมาก!

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    // แปะตรงนี้เลยสัส! เป็นท่าไม้ตายเอาไว้ให้ Spring Boot ค้นหาร้านจากชื่อ URL (Slug)
    Optional<Shop> findByShopSlug(String shopSlug);

}