package org.example.resellerbackend.controller;

import org.example.resellerbackend.dto.CheckoutRequest;
import org.example.resellerbackend.entity.OrderItem;
import org.example.resellerbackend.entity.Orders;
import org.example.resellerbackend.entity.Product;
import org.example.resellerbackend.entity.Shop;
import org.example.resellerbackend.entity.ShopProduct;
import org.example.resellerbackend.repository.OrderItemRepository;
import org.example.resellerbackend.repository.OrdersRepository;
import org.example.resellerbackend.repository.ProductRepository;
import org.example.resellerbackend.repository.ShopProductRepository;
import org.example.resellerbackend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopProductRepository shopProductRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    // ==========================================
    // 1. API ดึงข้อมูลหน้าร้าน
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

                org.example.resellerbackend.dto.CustomerProductResponse.ProductDetail detail =
                        new org.example.resellerbackend.dto.CustomerProductResponse.ProductDetail();
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
    // 2. API Checkout — รับหลายสินค้า สร้าง Order เดียว
    // ==========================================
    @Transactional
    @PostMapping("/shops/{slug}/checkout")
    public ResponseEntity<?> checkout(@PathVariable String slug, @RequestBody CheckoutRequest request) {

        Optional<Shop> shopOpt = shopRepository.findByShopSlug(slug);
        if (shopOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "ไม่พบร้านค้านี้!"));

        if (request.getItems() == null || request.getItems().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "ไม่มีสินค้าในตะกร้า!"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalQty = 0;

        // รอบแรก: validate + คำนวณ ก่อน (ยังไม่แตะ DB)
        List<Object[]> validated = new ArrayList<>();

        for (CheckoutRequest.CartItem cartItem : request.getItems()) {
            Optional<ShopProduct> spOpt = shopProductRepository.findById(cartItem.getShopProductId());
            if (spOpt.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบสินค้า id: " + cartItem.getShopProductId()));
            ShopProduct sp = spOpt.get();

            Optional<Product> prodOpt = productRepository.findById(sp.getProductId());
            if (prodOpt.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "ไม่พบข้อมูลสินค้ากลาง!"));
            Product product = prodOpt.get();

            if (product.getStock() < cartItem.getQuantity())
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "สินค้า \"" + product.getName() + "\" เหลือแค่ " + product.getStock() + " ชิ้น"
                ));

            BigDecimal itemTotal = sp.getSellingPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            BigDecimal itemCost  = product.getCostPrice().multiply(new BigDecimal(cartItem.getQuantity()));

            totalAmount = totalAmount.add(itemTotal);
            totalProfit = totalProfit.add(itemTotal.subtract(itemCost));
            totalQty   += cartItem.getQuantity();

            validated.add(new Object[]{ sp, product, cartItem.getQuantity() });
        }

        // สร้าง Order เดียว
        Orders order = new Orders();
        order.setShopId(shopOpt.get().getId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setQuantity(totalQty);
        order.setTotalAmount(totalAmount);
        order.setResellerProfit(totalProfit);
        order.setStatus("รอชำระเงิน");
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setCreatedAt(java.time.LocalDateTime.now());
        ordersRepository.save(order);

        // รอบสอง: ตัดสต็อก + บันทึก OrderItem ทุกรายการ
        for (Object[] row : validated) {
            ShopProduct sp      = (ShopProduct) row[0];
            Product     product = (Product)     row[1];
            int         qty     = (int)         row[2];

            product.setStock(product.getStock() - qty);
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setQuantity(qty);
            item.setCostPrice(product.getCostPrice());
            item.setSellingPrice(sp.getSellingPrice());
            orderItemRepository.save(item);
        }

        return ResponseEntity.ok(Map.of(
                "message",     "สร้างออเดอร์สำเร็จ!",
                "orderNumber", order.getOrderNumber(),
                "totalAmount", order.getTotalAmount()
        ));
    }

    // ==========================================
    // 3. API จ่ายเงินจำลอง
    // ==========================================
    @PostMapping("/orders/{orderNumber}/pay")
    public ResponseEntity<?> payOrder(@PathVariable String orderNumber) {
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "ไม่พบออเดอร์!"));

        Orders order = orderOpt.get();
        if (!order.getStatus().equals("รอชำระเงิน"))
            return ResponseEntity.badRequest().body(Map.of("message", "ออเดอร์นี้จ่ายไปแล้ว!"));

        order.setStatus("รอดำเนินการ");
        ordersRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message",   "จ่ายเงินสำเร็จ!",
                "newStatus", order.getStatus()
        ));
    }

    // ==========================================
    // 4. API ติดตามสถานะออเดอร์
    // ==========================================
    @GetMapping("/track-order/{orderNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderNumber) {
        Optional<Orders> orderOpt = ordersRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "ไม่พบเลขที่บิลนี้ในระบบ!"));

        Orders order = orderOpt.get();

        // ดึง OrderItem ทั้งหมดของ order นี้
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        List<Map<String, Object>> itemList = orderItems.stream().map(oi -> {
            Map<String, Object> item = new HashMap<>();
            item.put("quantity", oi.getQuantity());
            item.put("sellingPrice", oi.getSellingPrice());

            // ดึงชื่อ + รูปสินค้า
            productRepository.findById(oi.getProductId()).ifPresent(p -> {
                item.put("productName", p.getName());
                item.put("productImage", p.getImageUrl());
            });
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber",  order.getOrderNumber());
        response.put("customerName", order.getCustomerName());
        response.put("status",       order.getStatus());
        response.put("totalAmount",  order.getTotalAmount());
        response.put("quantity",     order.getQuantity());
        response.put("items",        itemList); // ✅ เพิ่ม
        return ResponseEntity.ok(response);
    }
}