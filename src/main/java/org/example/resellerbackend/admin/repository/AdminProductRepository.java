package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminProductEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminProductRepository extends CrudRepository<AdminProductEntity, Long> {

    // ค้นหาสินค้าจาก "ชื่อ" โดยไม่สนตัวพิมพ์เล็ก/ใหญ่ และคืนค่ามาเป็น List ธรรมดา
    List<AdminProductEntity> findByNameContainingIgnoreCase(String name);

}