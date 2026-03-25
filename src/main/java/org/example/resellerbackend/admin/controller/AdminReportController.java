package org.example.resellerbackend.admin.controller;

import org.example.resellerbackend.admin.entity.AdminOrderEntity;
import org.example.resellerbackend.admin.repository.AdminOrderRepository;
import org.example.resellerbackend.entity.Product;
import org.example.resellerbackend.entity.Shop;
import org.example.resellerbackend.repository.ProductRepository;
import org.example.resellerbackend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Autowired private AdminOrderRepository orderRepo;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ProductRepository productRepository;

    //หน้าreport admin
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        List<AdminOrderEntity> allOrders = orderRepo.findTop100ByOrderByIdDesc();

        // กรองเฉพาะออเดอร์ที่ shipped หรือ completed (ถือว่าขายได้แล้ว)
        List<AdminOrderEntity> doneOrders = allOrders.stream()
                .filter(o -> "shipped".equals(o.getStatus())
                        || "completed".equals(o.getStatus())
                        || "จัดส่งแล้ว".equals(o.getStatus()))
                .collect(Collectors.toList());

        // ยอดขายรวม
        BigDecimal totalSales = doneOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // กำไรตัวแทนรวม
        BigDecimal totalResellerProfit = doneOrders.stream()
                .map(o -> o.getResellerProfit() != null ? o.getResellerProfit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ต้นทุน Admin = ยอดขาย - กำไรตัวแทน
        BigDecimal adminRevenue = totalSales.subtract(totalResellerProfit);

        // จำนวนออเดอร์จัดส่งแล้ว
        long deliveredCount = doneOrders.size();

        // ยอดขายแต่ละร้าน
        Map<Long, Map<String, Object>> shopMap = new HashMap<>();
        for (AdminOrderEntity order : doneOrders) {
            Long shopId = order.getShopId();
            if (shopId == null) continue;

            shopMap.computeIfAbsent(shopId, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("shopId", k);
                m.put("orderCount", 0);
                m.put("sales", BigDecimal.ZERO);
                m.put("profit", BigDecimal.ZERO);

                // ดึงชื่อร้าน
                shopRepository.findById(k).ifPresent(shop ->
                        m.put("shopName", shop.getShopName())
                );
                if (!m.containsKey("shopName")) m.put("shopName", "ร้าน #" + k);

                return m;
            });

            Map<String, Object> shopData = shopMap.get(shopId);
            shopData.put("orderCount", (int) shopData.get("orderCount") + 1);
            shopData.put("sales", ((BigDecimal) shopData.get("sales"))
                    .add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO));
            shopData.put("profit", ((BigDecimal) shopData.get("profit"))
                    .add(order.getResellerProfit() != null ? order.getResellerProfit() : BigDecimal.ZERO));
        }

        List<Map<String, Object>> shopSales = new ArrayList<>(shopMap.values());
        shopSales.sort((a, b) -> ((BigDecimal) b.get("sales")).compareTo((BigDecimal) a.get("sales")));

        // สินค้าขายดี (จาก product name ใน order — ใช้ shopId + productId จาก orders)
        // เนื่องจาก AdminOrderEntity ไม่มี productId โดยตรง ให้นับจาก quantity ต่อร้าน
        // ถ้าอยากได้สินค้าขายดีจริงๆ ต้องมี field productId ใน orders table
        // ตอนนี้ใช้ productRepository ดึงสินค้าที่ stock ลดลงมากที่สุดแทน
        List<Product> products = productRepository.findAll();
        // เรียงตาม stock น้อยสุด (ขายออกไปเยอะสุด) — approximation
        products.sort(Comparator.comparingInt(Product::getStock));
        List<Map<String, Object>> bestSellers = products.stream().limit(5).map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("stock", p.getStock());
            m.put("imageUrl", p.getImageUrl());
            return m;
        }).collect(Collectors.toList());

        // Response
        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", totalSales);
        result.put("adminRevenue", adminRevenue);
        result.put("totalResellerProfit", totalResellerProfit);
        result.put("deliveredCount", deliveredCount);
        result.put("shopSales", shopSales);
        result.put("bestSellers", bestSellers);

        return ResponseEntity.ok(result);
    }
}