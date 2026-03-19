package org.example.resellerbackend.controller;

import org.example.resellerbackend.dto.CheckoutRequest;
import org.example.resellerbackend.entity.Orders;
import org.example.resellerbackend.entity.Product;
import org.example.resellerbackend.entity.Shop;
import org.example.resellerbackend.entity.ShopProduct;
import org.example.resellerbackend.repository.OrdersRepository;
import org.example.resellerbackend.repository.ProductRepository;
import org.example.resellerbackend.repository.ShopProductRepository;
import org.example.resellerbackend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopProductRepository shopProductRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrdersRepository ordersRepository;

    // ==========================================
    // 1. API ดึงข้อมูลหน้าร้าน (BR-24)
    // ==========================================
    @GetMapping("/shops/{slug}")
    public ResponseEntity<?> getShopBySlug(@PathVariable String slug) {
        Optional<Shop> shopOpt = shopRepository.findByShopSlug(slug);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "ไม่พบร้านค้านี้!"));
        }

        Shop shop = shopOpt.get();
        List<ShopProduct> shopProducts = shopProductRepository.findByShopId(shop.getId());

        List<org.example.resellerbackend.dto.CustomerProductResponse> responseProducts = shopProducts.stream().map(sp -> {
            org.example.resellerbackend.dto.CustomerProductResponse res = new org.example.resellerbackend.dto.CustomerProductResponse();
            res.setId(sp.getId());
            res.setProductId(sp.getProductId());
            res.setShopId(sp.getShopId());
            res.setSellingPrice(sp.getSellingPrice());

            Optional<Product> prodOpt = productRepository.findById(sp.getProductId());
            if (prodOpt.isPresent()) {
                Product p = prodOpt.get();
                res.setStock(p.getStock());

                org.example.resellerbackend.dto.CustomerProductResponse.ProductDetail detail = new org.example.resellerbackend.dto.CustomerProductResponse.ProductDetail();
                detail.setProductName(p.getName());
                detail.setProductImage(p.getImageUrl());
                detail.setStock(p.getStock());
                res.setProduct(detail);
            }
            return res;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("shopId", shop.getId());
        response.put("shopName", shop.getShopName());
        response.put("products", responseProducts);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. API สั่งซื้อสินค้า Checkout (BR-26, BR-27)
    // ==========================================
    @Transactional  // ✅ เพิ่ม @Transactional เพื่อความปลอดภัย
    @PostMapping("/shops/{slug}/checkout")
    public ResponseEntity<?> checkout(@PathVariable String slug, @RequestBody CheckoutRequest request) {

        Optional<Shop> shopOpt = shopRepository.findByShopSlug(slug);
        if (shopOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "ไม่พบร้านค้านี้!"));

        Optional<ShopProduct> spOpt = shopProductRepository.findById(request.getShopProductId());
        if (spOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "ไม่มีสินค้านี้ในร้าน!"));
        ShopProduct sp = spOpt.get();

        // ✅ ดึง product จาก sp.getProductId() (ไม่ใช่ hardcode!)
        Optional<Product> prodOpt = productRepository.findById(sp.getProductId());
        if (prodOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบข้อมูลสินค้ากลาง!"));
        Product product = prodOpt.get();

        // ✅ เช็คสต็อก (BR-27)
        if (product.getStock() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "สินค้าหมดแล้ว!"));
        }
        if (request.getQuantity() > product.getStock()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "สินค้าไม่เพียงพอ เหลือแค่ " + product.getStock() + " ชิ้น"
            ));
        }

        // คำนวณเงิน
        BigDecimal totalAmount = sp.getSellingPrice().multiply(new BigDecimal(request.getQuantity()));
        BigDecimal costAmount = product.getCostPrice().multiply(new BigDecimal(request.getQuantity()));
        BigDecimal profit = totalAmount.subtract(costAmount);

        // ✅ ตัดสต็อกตอน checkout เลย (ไม่รอ pay)
        product.setStock(product.getStock() - request.getQuantity());
        productRepository.save(product);

        // สร้างออเดอร์
        Orders order = new Orders();
        order.setShopId(shopOpt.get().getId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setResellerProfit(profit);
        order.setStatus("รอชำระเงิน");
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setCreatedAt(java.time.LocalDateTime.now());

        // ✅ บันทึก product ID ไว้ใน order เพื่อใช้คืน stock ถ้า cancel
        // (ถ้า Orders entity มี field productId ให้เพิ่ม: order.setProductId(product.getId());)

        ordersRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "สร้างออเดอร์สำเร็จ ไปหน้าจ่ายเงินได้เลย!");
        response.put("orderNumber", order.getOrderNumber());
        response.put("totalAmount", order.getTotalAmount());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // API จ่ายเงินจำลอง (BR-28, BR-29)
    // ✅ แก้แล้ว: ไม่ต้องตัดสต็อกซ้ำอีก เพราะตัดตอน checkout แล้ว
    // ==========================================
    @PostMapping("/orders/{orderNumber}/pay")
    public ResponseEntity<?> payOrder(@PathVariable String orderNumber) {
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "ไม่พบออเดอร์!"));

        Orders order = orderOpt.get();
        if (!order.getStatus().equals("รอชำระเงิน")) {
            return ResponseEntity.badRequest().body(Map.of("message", "ออเดอร์นี้จ่ายไปแล้ว!"));
        }

        // ✅ ไม่ต้องตัดสต็อกซ้ำ (ตัดตอน checkout แล้ว)
        // เปลี่ยนสถานะเป็น รอดำเนินการ (BR-28)
        order.setStatus("รอดำเนินการ");
        ordersRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "จ่ายเงินสำเร็จ!",
                "newStatus", order.getStatus()
        ));
    }

    // ==========================================
    // 3. API ติดตามสถานะออเดอร์ (BR-30, BR-31)
    // ==========================================
    @GetMapping("/track-order/{orderNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderNumber) {
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "ไม่พบเลขที่บิลนี้ในระบบ! เช็คเลขดีๆ หน่อย!"
            ));
        }

        Orders order = orderOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber", order.getOrderNumber());
        response.put("customerName", order.getCustomerName());
        response.put("status", order.getStatus());
        response.put("totalAmount", order.getTotalAmount());
        response.put("quantity", order.getQuantity());

        return ResponseEntity.ok(response);
    }
}