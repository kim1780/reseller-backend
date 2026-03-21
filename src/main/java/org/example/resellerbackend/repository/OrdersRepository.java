package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByShopIdOrderByCreatedAtDesc(Long shopId);

    // เพิ่มบรรทัดนี้ไว้หาออเดอร์ตอนจ่ายเงินสัส!
    Optional<Orders> findByOrderNumber(String orderNumber);

    // Pagination: ดึงออเดอร์เรียงจากใหม่ไปเก่า
    Page<Orders> findAllByOrderByIdDesc(Pageable pageable);

    // ค้นหา + Pagination
    @Query("SELECT o FROM Orders o WHERE " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY o.id DESC")
    Page<Orders> searchOrders(@Param("search") String search, Pageable pageable);
}