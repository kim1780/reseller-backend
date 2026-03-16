package org.example.resellerbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ShopProductRequest {
    private Long shopId;       // ไอดีร้านของเพื่อนมึง
    private Long productId;    // ไอดีสินค้าจาก Catalog
    private BigDecimal price;  // ราคาที่ Reseller ตั้งใจจะขาย (ปุ่มแก้ไขราคา)
}