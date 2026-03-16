package org.example.resellerbackend.controller;

import org.example.resellerbackend.entity.*;
import org.example.resellerbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
        if (shop == null) return ResponseEntity.badRequest().body("ไม่พบร้านค้านี้");

        Long userId = shop.getUserId();
        Wallet wallet = walletRepository.findById(userId).orElse(new Wallet());

        // ใช้ฟังก์ชันดึงประวัติเรียงจากใหม่ไปเก่า
        List<WalletLog> history = walletLogRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return ResponseEntity.ok(new WalletDTO(wallet.getBalance(), history));
    }

    // 3. เพิ่มสินค้าเข้าหน้าร้าน (แก้ตามกฎ BR-19 ห้ามต่ำกว่า Min Price)
    @PostMapping("/shops/{shopId}/products")
    public ResponseEntity<?> addProductToShop(@PathVariable Long shopId, @RequestBody ShopProduct request) {
        if (!shopRepository.existsById(shopId)) {
            return ResponseEntity.badRequest().body("ไม่เจอรหัสร้านค้า " + shopId + " เช็ค DBeaver สัส!");
        }

        return productRepository.findById(request.getProductId())
                .map(central -> {
                    // [ไฮไลท์!] เช็คราคาขายห้ามต่ำกว่า Min Price (กฎ BR-19)
                    if (request.getSellingPrice().compareTo(central.getMinPrice()) < 0) {
                        return ResponseEntity.badRequest().body("ห้ามขายต่ำกว่าราคาขั้นต่ำ: " + central.getMinPrice() + " บาทสัสคิม!");
                    }

                    request.setShopId(shopId);
                    request.setStatus("active");

                    try {
                        return ResponseEntity.ok(shopProductRepository.save(request));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("DB ระเบิด: " + e.getMessage());
                    }
                }).orElse(ResponseEntity.badRequest().body("ไม่เจอสินค้า ID " + request.getProductId()));
    }

    // 4. แก้ไขราคาขาย (เช็คกฎ BR-19 เหมือนกัน)
    @PutMapping("/shops/{shopId}/products/{shopProductId}")
    public ResponseEntity<?> updateShopProductPrice(
            @PathVariable Long shopId,
            @PathVariable Long shopProductId,
            @RequestBody ShopProduct request) {

        return shopProductRepository.findById(shopProductId).map(existing -> {
            Product central = productRepository.findById(existing.getProductId()).orElse(null);

            // [ไฮไลท์!] ต้องเช็คกับ Min Price นะสัส ไม่ใช่ Cost Price
            if (central != null && request.getSellingPrice().compareTo(central.getMinPrice()) < 0) {
                return ResponseEntity.badRequest().body("ราคาห้ามต่ำกว่าราคาขั้นต่ำ " + central.getMinPrice() + " บาท!");
            }
            existing.setSellingPrice(request.getSellingPrice());
            return ResponseEntity.ok(shopProductRepository.save(existing));
        }).orElse(ResponseEntity.badRequest().body("ไม่พบสินค้าที่จะแก้ไข"));
    }

    // 5. ลบสินค้าออกจากร้าน (สำหรับปุ่ม ถังขยะ)
    @DeleteMapping("/shops/{shopId}/products/{shopProductId}")
    public ResponseEntity<?> deleteShopProduct(
            @PathVariable Long shopId,
            @PathVariable Long shopProductId) {

        if (shopProductRepository.existsById(shopProductId)) {
            shopProductRepository.deleteById(shopProductId);
            return ResponseEntity.ok("ลบสินค้าเรียบร้อยแล้ว");
        }
        return ResponseEntity.badRequest().body("ไม่พบสินค้าที่จะลบ");
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
    //8
    @Transactional
    @PostMapping("/shops/{shopId}/orders")
    public ResponseEntity<?> createOrder(@PathVariable Long shopId, @RequestBody Orders order) {
        // ใน Postman มึงส่ง "id": 1 มา
        // เราจะเอาเลข id นั้นแหละมาค้นหาสินค้าเพื่อตัดสต็อก
        return productRepository.findById(order.getId()).map(product -> {
            if (product.getStock() <= 0) {
                return ResponseEntity.badRequest().body("สินค้าหมดแล้วสัส!");
            }

            // 1. ตัดสต็อกตัวที่เลือกจริง ๆ
            product.setStock(product.getStock() - 1);
            productRepository.save(product);

            // 2. ตั้งค่าออเดอร์
            order.setShopId(shopId);
            order.setOrderNumber("ORD-" + System.currentTimeMillis());
            order.setCreatedAt(java.time.LocalDateTime.now());
            order.setStatus("pending");

            // 3. บันทึกออเดอร์
            return ResponseEntity.ok(ordersRepository.save(order));
        }).orElse(ResponseEntity.badRequest().body("ไม่เจอสินค้า ID " + order.getId() + " ในระบบนะมิมก์"));
    }

    // 9. อัปเดตสถานะออเดอร์ และเอาเงินเข้า Wallet
    // 9. อัปเดตสถานะออเดอร์ และเอาเงินเข้า Wallet (ฉบับรวมร่างกันเหนียว)
    @Transactional
    @PutMapping("/shops/{shopId}/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long shopId,
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> request) {

        String newStatus = request.get("status");

        return ordersRepository.findById(orderId).map(order -> {
            // ป้องกันการคิดเงินซ้ำ
            if ("completed".equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.badRequest().body("ออเดอร์นี้จัดส่งและคิดกำไรไปแล้วสัส!");
            }

            order.setStatus(newStatus);
            ordersRepository.save(order);

            // เอาเงินเข้า Wallet เมื่อ status เป็น completed
            if ("completed".equalsIgnoreCase(newStatus)) {
                return shopRepository.findById(shopId).map(shop -> {
                    Long userId = shop.getUserId();

                    // 1. บันทึก Log ประวัติกำไร
                    WalletLog log = new WalletLog();
                    log.setUserId(userId);
                    log.setOrderId(order.getId());
                    log.setAmount(order.getResellerProfit() != null ? order.getResellerProfit() : BigDecimal.ZERO);
                    log.setCreatedAt(java.time.LocalDateTime.now());
                    walletLogRepository.save(log);

                    // 2. จัดการ Wallet (ถ้าไม่เจอให้สร้างใหม่ ป้องกัน 500)
                    Wallet wallet = walletRepository.findById(userId).orElseGet(() -> {
                        Wallet newW = new Wallet();
                        newW.setUserId(userId);
                        newW.setBalance(BigDecimal.ZERO);
                        return walletRepository.save(newW);
                    });

                    // 3. บวกเงิน
                    BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
                    BigDecimal profit = order.getResellerProfit() != null ? order.getResellerProfit() : BigDecimal.ZERO;
                    wallet.setBalance(currentBalance.add(profit));
                    walletRepository.save(wallet);

                    return ResponseEntity.ok("อัปเดตสถานะและโอนกำไรเข้า Wallet เรียบร้อยแล้ว!");
                }).orElse(ResponseEntity.badRequest().body("ไม่เจอรหัสร้านค้า " + shopId));
            }

            return ResponseEntity.ok("อัปเดตสถานะเป็น " + newStatus + " เรียบร้อยแล้ว");
        }).orElse(ResponseEntity.badRequest().body("ไม่พบออเดอร์ ID " + orderId));
    }

    // 10. API สำหรับหน้า Dashboard (รวบยอด)
    @GetMapping("/shops/{shopId}/dashboard")
    public ResponseEntity<?> getDashboardSummary(@PathVariable Long shopId) {

        Shop shop = shopRepository.findById(shopId).orElse(null);
        BigDecimal totalProfit = BigDecimal.ZERO;

        // ดึงยอดเงินจาก Wallet
        if (shop != null) {
            Wallet wallet = walletRepository.findById(shop.getUserId()).orElse(null);
            if (wallet != null && wallet.getBalance() != null) {
                totalProfit = wallet.getBalance();
            }
        }

        List<Orders> allOrders = ordersRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        long totalOrdersCount = allOrders.size();
        long pendingOrdersCount = allOrders.stream().filter(o -> !"completed".equals(o.getStatus())).count();

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

// ================= [DTO Classes] =================
class WalletDTO {
    public BigDecimal totalBalance;
    public List<WalletLog> history;
    public WalletDTO(BigDecimal b, List<WalletLog> h) { this.totalBalance = b; this.history = h; }
}

class ShopProductResponse {
    public Long shopProductId; public Long productId; public String productName;
    public String category; public String imageUrl; public BigDecimal costPrice;
    public BigDecimal minPrice; public BigDecimal sellingPrice; public Integer stock; public String status;

    public ShopProductResponse(Long spId, Long pId, String pName, String cat, String img, BigDecimal cost, BigDecimal min, BigDecimal sell, Integer st, String stat) {
        this.shopProductId = spId; this.productId = pId; this.productName = pName; this.category = cat; this.imageUrl = img;
        this.costPrice = cost; this.minPrice = min; this.sellingPrice = sell; this.stock = st; this.status = stat;
    }
}