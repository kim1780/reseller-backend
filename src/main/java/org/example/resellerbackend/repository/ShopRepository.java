package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    // ไม่ต้องเขียนอะไรเพิ่ม Spring Boot จัดการให้หมดแล้ว!
}