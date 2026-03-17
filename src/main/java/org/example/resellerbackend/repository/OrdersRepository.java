package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByShopIdOrderByCreatedAtDesc(Long shopId);

    // เพิ่มบรรทัดนี้ไว้หาออเดอร์ตอนจ่ายเงินสัส!
    Optional<Orders> findByOrderNumber(String orderNumber);
}