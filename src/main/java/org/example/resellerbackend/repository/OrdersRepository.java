package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    // เปลี่ยนจาก OrderDate เป็น CreatedAt
    List<Orders> findByShopIdOrderByCreatedAtDesc(Long shopId);
}