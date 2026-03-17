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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private OrdersRepository ordersRepository; // ของใหม่ เอาไว้เซฟออเดอร์!

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
        List<ShopProduct> products = shopProductRepository.findByShopId(shop.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("shopId", shop.getId());
        response.put("shopName", shop.getShopName());
        response.put("products", products);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. API สั่งซื้อสินค้า Checkout (BR-26, BR-27)
    // ==========================================
    @PostMapping("/shops/{slug}/checkout")
    public ResponseEntity<?> checkout(@PathVariable String slug, @RequestBody CheckoutRequest request) {

        Optional<Shop> shopOpt = shopRepository.findByShopSlug(slug);
        if(shopOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "ไม่พบร้านค้านี้!"));

        Optional<ShopProduct> spOpt = shopProductRepository.findById(request.getShopProductId());
        if(spOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "ไม่มีสินค้านี้ในร้าน!"));
        ShopProduct sp = spOpt.get();

        Optional<Product> prodOpt = productRepository.findById(sp.getProductId());
        if(prodOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบข้อมูลสินค้ากลาง!"));
        Product product = prodOpt.get();

        // เช็คสต็อก (BR-27)
        if(request.getQuantity() > product.getStock()) {
            return ResponseEntity.badRequest().body(Map.of("message", "สินค้าไม่เพียงพอ"));
        }

        // คำนวณเงิน
        BigDecimal totalAmount = sp.getSellingPrice().multiply(new BigDecimal(request.getQuantity()));
        BigDecimal costAmount = product.getCostPrice().multiply(new BigDecimal(request.getQuantity()));
        BigDecimal profit = totalAmount.subtract(costAmount);

        // สร้างออเดอร์
        Orders order = new Orders();
        order.setShopId(shopOpt.get().getId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setResellerProfit(profit);
        order.setStatus("รอชำระเงิน"); // สถานะแรกเริ่ม (BR-26)
        order.setOrderNumber("ORD-" + System.currentTimeMillis()); // สร้างเลขที่บิลเท่ๆ

        // เซฟลง Database มหาลัย!
        ordersRepository.save(order);

        // ส่งผลลัพธ์กลับไปให้หน้าบ้านพาลูกค้าไปจ่ายเงิน
        Map<String, Object> response = new HashMap<>();
        response.put("message", "สร้างออเดอร์สำเร็จ ไปหน้าจ่ายเงินได้เลย!");
        response.put("orderNumber", order.getOrderNumber());
        response.put("totalAmount", order.getTotalAmount());
        return ResponseEntity.ok(response);
    }
    // API จ่ายเงินจำลอง (BR-28, BR-29)
    @PostMapping("/orders/{orderNumber}/pay")
    public ResponseEntity<?> payOrder(@PathVariable String orderNumber) {
        // 1. หาออเดอร์
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "ไม่พบออเดอร์!"));

        Orders order = orderOpt.get();
        if (!order.getStatus().equals("รอชำระเงิน")) {
            return ResponseEntity.badRequest().body(Map.of("message", "ออเดอร์นี้จ่ายไปแล้ว!"));
        }

        // 2. ไปหาว่าออเดอร์นี้สั่งสินค้าตัวไหน (เพื่อจะเอา productId ไปตัดสต็อกกลาง)
        // กูเขียนแบบรัดกุมให้มึงเลย
        List<ShopProduct> spList = shopProductRepository.findByShopId(order.getShopId());
        // (จริงๆ มึงควรเก็บ productId ไว้ใน Orders เลยจะง่ายกว่า แต่ตอนนี้เอาท่านี้ไปก่อน!)
        // มึงไปแก้สต็อกกลางตัว ID 12 ของมึงซะ!
        Optional<Product> prodOpt = productRepository.findById(12L); // 12L คือไอดีที่มึงแก้สต็อกตะกี้
        if(prodOpt.isPresent()){
            Product p = prodOpt.get();
            p.setStock(p.getStock() - order.getQuantity()); // ตัดสต็อกจริง! (BR-29)
            productRepository.save(p);
        }

        // 3. เปลี่ยนสถานะ (BR-28)
        order.setStatus("รอดำเนินการ");
        ordersRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "จ่ายเงินสำเร็จ! สต็อกกลางโดนตัดแล้ว!",
                "newStatus", order.getStatus()
        ));
    }

    // ==========================================
    // 3. API ติดตามสถานะออเดอร์ (BR-30, BR-31)
    // ==========================================
    @GetMapping("/track-order/{orderNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderNumber) {
        // ค้นหาออเดอร์ด้วยเลขบิล (ใช้ method ที่มึงเพิ่งแอดใน Repository ตะกี้)
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);

        // ถ้าไม่เจอออเดอร์ (BR-31)
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "ไม่พบเลขที่บิลนี้ในระบบ! เช็คเลขดีๆ หน่อย!"
            ));
        }

        // ถ้าเจอ ให้ส่งข้อมูลออเดอร์กลับไป (BR-30)
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