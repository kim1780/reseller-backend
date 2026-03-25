package org.example.resellerbackend.controller;

import org.example.resellerbackend.dto.ShopProductResponse;
import org.example.resellerbackend.entity.*;
import org.example.resellerbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reseller")
@CrossOrigin(origins = "*")
public class ResellerProductController {

    @Autowired private ShopProductRepository shopProductRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletLogRepository walletLogRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    // 1. ดึงสินค้าในร้าน (หน้า ร้านของฉัน)
    @GetMapping("/shops/{shopId}/products")
    public ResponseEntity<?> getMyProducts(@PathVariable Long shopId) {
        List<ShopProduct> shopProducts = shopProductRepository.findByShopId(shopId);
        List<ShopProductResponse> response = shopProducts.stream().map(sp -> {
            Product p = productRepository.findById(sp.getProductId()).orElse(new Product());
            return new ShopProductResponse(
                    sp.getId(), p.getId(), p.getName(), p.getCategory(), p.getImageUrl(),
                    p.getCostPrice(), p.getMinPrice(), sp.getSellingPrice(), p.getStock(), sp.getStatus()
            );
        }).toList();
        return ResponseEntity.ok(response);
    }

    // 2. ดึงยอดเงินและประวัติกำไร (หน้า Wallet)
    @GetMapping("/shops/{shopId}/wallet")
    public ResponseEntity<?> getWallet(@PathVariable Long shopId) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบร้านค้านี้"));

        Long userId = shop.getUserId();
        Wallet wallet = walletRepository.findById(userId).orElse(new Wallet());

        // ใช้ฟังก์ชันดึงประวัติเรียงจากใหม่ไปเก่า
        List<WalletLog> history = walletLogRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return ResponseEntity.ok(new WalletDTO(wallet.getBalance(), history));
    }

    // 3. เพิ่มสินค้าเข้าหน้าร้าน (ห้ามต่ำกว่า Min Price)
    @PostMapping("/shops/{shopId}/products")
    public ResponseEntity<?> addProductToShop(@PathVariable Long shopId, @RequestBody ShopProduct request) {
        if (!shopRepository.existsById(shopId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "ไม่เจอรหัสร้านค้า " + shopId + " เช็ค DBeaver !"));
        }

        return productRepository.findById(request.getProductId())
                .map(central -> {
                    // [ไฮไลท์!] เช็คราคาขายห้ามต่ำกว่า Min Price (กฎ BR-19)
                    if (request.getSellingPrice().compareTo(central.getMinPrice()) < 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "ห้ามขายต่ำกว่าราคาขั้นต่ำ: " + central.getMinPrice() + " บาท!"));
                    }

                    request.setShopId(shopId);
                    request.setStatus("active");

                    try {
                        return ResponseEntity.ok(shopProductRepository.save(request));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body(Map.of("message", "DB ระเบิด: " + e.getMessage()));
                    }
                }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่เจอสินค้า ID " + request.getProductId())));
    }

    // 4. แก้ไขราคาขาย
    @PutMapping("/shops/{shopId}/products/{shopProductId}")
    public ResponseEntity<?> updateShopProductPrice(
            @PathVariable Long shopId,
            @PathVariable Long shopProductId,
            @RequestBody ShopProduct request) {

        return shopProductRepository.findById(shopProductId).map(existing -> {
            Product central = productRepository.findById(existing.getProductId()).orElse(null);

            // [ไฮไลท์!] ต้องเช็คกับ Min Price นะ ไม่ใช่ Cost Price
            if (central != null && request.getSellingPrice().compareTo(central.getMinPrice()) < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ราคาห้ามต่ำกว่าราคาขั้นต่ำ " + central.getMinPrice() + " บาท!"));
            }
            existing.setSellingPrice(request.getSellingPrice());
            return ResponseEntity.ok(shopProductRepository.save(existing));
        }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่พบสินค้าที่จะแก้ไข")));
    }

    // 5. ลบสินค้าออกจากร้าน (สำหรับปุ่ม ถังขยะ)
    @DeleteMapping("/shops/{shopId}/products/{shopProductId}")
    public ResponseEntity<?> deleteShopProduct(
            @PathVariable Long shopId,
            @PathVariable Long shopProductId) {

        if (shopProductRepository.existsById(shopProductId)) {
            shopProductRepository.deleteById(shopProductId);
            return ResponseEntity.ok(Map.of("message", "ลบสินค้าเรียบร้อยแล้ว"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบสินค้าที่จะลบ"));
    }

    // 6. ดึงรายการสินค้ากลางทั้งหมด (หน้า สินค้ากลาง)
    @GetMapping("/products")
    public ResponseEntity<?> getAllCentralProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // 7. ดึงรายการออเดอร์ทั้งหมดของร้าน (หน้า ออเดอร์)
    @GetMapping("/shops/{shopId}/orders")
    public ResponseEntity<?> getShopOrders(@PathVariable Long shopId) {
        return ResponseEntity.ok(ordersRepository.findByShopIdOrderByCreatedAtDesc(shopId));
    }

    // [ไฮไลท์!] 7.1 ดึงรายการสินค้าในออเดอร์ (แก้ให้ Reseller ดูรูปได้)
    @GetMapping("/shops/{shopId}/orders/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Long shopId, @PathVariable Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<Map<String, Object>> result = items.stream().map(item -> {
            String productName = "สินค้า #" + item.getProductId();
            String productImage = "";
            Optional<Product> prodOpt = productRepository.findById(item.getProductId());
            if (prodOpt.isPresent()) {
                productName = prodOpt.get().getName();
                productImage = prodOpt.get().getImageUrl() != null ? prodOpt.get().getImageUrl() : "";
            }
            return Map.<String, Object>of(
                    "id", item.getId(),
                    "productId", item.getProductId(),
                    "productName", productName,
                    "productImage", productImage,
                    "quantity", item.getQuantity(),
                    "costPrice", item.getCostPrice(),
                    "sellingPrice", item.getSellingPrice()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    // 8. สร้างออเดอร์และตัดสต็อก (คำนวณกำไรเอง ห้ามไว้ใจ Frontend)
    @Transactional
    @PostMapping("/shops/{shopId}/orders")
    public ResponseEntity<?> createOrder(@PathVariable Long shopId, @RequestBody Orders order) {
        // order.getId() ที่มึงส่งมาคือ ID สินค้ากลาง
        return productRepository.findById(order.getId()).map(product -> {

            // หาว่าร้านนี้ (shopId) เอาสินค้านี้ไปตั้งราคาขายไว้กี่บาทในตาราง shop_products
            ShopProduct shopProduct = shopProductRepository.findByShopId(shopId).stream()
                    .filter(sp -> sp.getProductId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            if (shopProduct == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "ยังไม่ได้เพิ่มสินค้านี้เข้าร้านเลย จะขายได้ไง!"));
            }

            // สมมติว่าถ้าไม่มีการส่งจำนวนซื้อ (quantity) มา ให้ถือว่าซื้อ 1 ชิ้น
            int qty = (order.getQuantity() != null && order.getQuantity() > 0) ? order.getQuantity() : 1;

            if (product.getStock() < qty) {
                return ResponseEntity.badRequest().body(Map.of("message", "สินค้าไม่พอขาย! เหลือแค่ " + product.getStock() + " ชิ้น"));
            }

            // ==========================================
            // 🔥 คำนวณกำไรหลังบ้าน 🔥
            // สูตร: กำไร = (ราคาขาย - ราคาทุน) x จำนวน
            // ==========================================
            BigDecimal costPrice = product.getCostPrice();          // ทุน (จากคลังกลาง)
            BigDecimal sellingPrice = shopProduct.getSellingPrice(); // ราคาขาย (ที่ Reseller ตั้ง)

            BigDecimal profitPerItem = sellingPrice.subtract(costPrice); // กำไรต่อชิ้น
            BigDecimal totalProfit = profitPerItem.multiply(BigDecimal.valueOf(qty)); // กำไรรวม

            BigDecimal totalAmount = sellingPrice.multiply(BigDecimal.valueOf(qty)); // ยอดขายรวม (ราคา x จำนวน)

            // 1. ตัดสต็อกตามจำนวนที่สั่งจริง
            product.setStock(product.getStock() - qty);
            productRepository.save(product);

            // 2. ตั้งค่าออเดอร์ (เซ็ตค่าเงินทับสิ่งที่ Frontend ส่งมาไปเลย!)
            order.setShopId(shopId);
            order.setOrderNumber("ORD-" + System.currentTimeMillis());
            order.setTotalAmount(totalAmount);      // ยอดขายรวม
            order.setResellerProfit(totalProfit);// กำไรของ Reseller
            order.setQuantity(qty); // ยัดจำนวนชิ้นที่คำนวณแล้วลงไป
            order.setCustomerName(order.getCustomerName());
            order.setCreatedAt(java.time.LocalDateTime.now());
            order.setStatus("pending");

            // 3. บันทึกออเดอร์ลง DB
            return ResponseEntity.ok(ordersRepository.save(order));
        }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่เจอสินค้า ID " + order.getId() + " ในระบบคลังกลางนะ")));
    }

    // 9. อัปเดตสถานะออเดอร์ (เปลี่ยนเป็นภาษา)
    @Transactional
    @PutMapping("/shops/{shopId}/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long shopId,
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> request) {

        String newStatus = request.get("status");

        return ordersRepository.findById(orderId).map(order -> {
            // [จุดที่ 1] ป้องกันการคิดเงินซ้ำ (เปลี่ยนเป็นภาษาไทย!)
            if ("จัดส่งแล้ว".equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "ออเดอร์นี้จัดส่งและคิดกำไรไปแล้ว!"));
            }

            order.setStatus(newStatus);
            ordersRepository.save(order);

            // [จุดที่ 2] เอาเงินเข้า Wallet เมื่อ status เป็น จัดส่งแล้ว
            if ("จัดส่งแล้ว".equalsIgnoreCase(newStatus)) {
                return shopRepository.findById(shopId).map(shop -> {
                    Long userId = shop.getUserId();

                    WalletLog log = new WalletLog();
                    log.setUserId(userId);
                    log.setOrderId(order.getId());
                    log.setAmount(order.getResellerProfit() != null ? order.getResellerProfit() : BigDecimal.ZERO);
                    log.setCreatedAt(java.time.LocalDateTime.now());
                    walletLogRepository.save(log);

                    Wallet wallet = walletRepository.findById(userId).orElseGet(() -> {
                        Wallet newW = new Wallet();
                        newW.setUserId(userId);
                        newW.setBalance(BigDecimal.ZERO);
                        return walletRepository.save(newW);
                    });

                    BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
                    BigDecimal profit = order.getResellerProfit() != null ? order.getResellerProfit() : BigDecimal.ZERO;
                    wallet.setBalance(currentBalance.add(profit));
                    walletRepository.save(wallet);

                    return ResponseEntity.ok(Map.of("message", "อัปเดตสถานะและโอนกำไรเข้า Wallet เรียบร้อยแล้ว!"));
                }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่เจอรหัสร้านค้า " + shopId)));
            }

            return ResponseEntity.ok(Map.of("message", "อัปเดตสถานะเป็น " + newStatus + " เรียบร้อยแล้ว"));
        }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่พบออเดอร์ ID " + orderId)));
    }

    // 10. API Dashboard (นับออเดอร์ค้าง)
    @GetMapping("/shops/{shopId}/dashboard")
    public ResponseEntity<?> getDashboardSummary(@PathVariable Long shopId) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        BigDecimal totalProfit = BigDecimal.ZERO;

        if (shop != null) {
            Wallet wallet = walletRepository.findById(shop.getUserId()).orElse(null);
            if (wallet != null && wallet.getBalance() != null) {
                totalProfit = wallet.getBalance();
            }
        }

        List<Orders> allOrders = ordersRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        long totalOrdersCount = allOrders.size();

        // [จุดที่ 3] แก้เงื่อนไขนับออเดอร์ค้างให้ตรงกัน!
        long pendingOrdersCount = allOrders.stream().filter(o -> !"จัดส่งแล้ว".equals(o.getStatus())).count();

        long totalProductsCount = shopProductRepository.findByShopId(shopId).size();
        List<Orders> recentOrders = allOrders.stream().limit(3).toList();

        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();
        dashboard.put("totalProfit", totalProfit);
        dashboard.put("pendingOrders", pendingOrdersCount);
        dashboard.put("totalProducts", totalProductsCount);
        dashboard.put("totalOrders", totalOrdersCount);
        dashboard.put("recentOrders", recentOrders);

        return ResponseEntity.ok(dashboard);
    }
}

