package org.example.resellerbackend.admin.controller;

import org.example.resellerbackend.admin.service.AdminService;
import org.example.resellerbackend.entity.OrderItem;
import org.example.resellerbackend.entity.Product;
import org.example.resellerbackend.repository.OrderItemRepository;
import org.example.resellerbackend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminService adminService;

    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;

    public AdminOrderController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(adminService.getOrdersPaginated(page, size, search));
    }

    // ✅ ดึงรายการสินค้าในออเดอร์ พร้อมชื่อสินค้า
    @GetMapping("/{id}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Long id) {
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
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
    // ปุ่มจดส่งหน้าเเอดมิน
    @PutMapping("/{id}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long id) {
        try {
            adminService.shipOrder(id);
            return ResponseEntity.ok(Map.of("message", "จัดส่งและโอนกำไรเข้ากระเป๋าตัวแทนสำเร็จ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    // จัดส่งสำเร็จ
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long id) {
        adminService.completeOrder(id);
        return ResponseEntity.ok(Map.of("message", "เปลี่ยนสถานะออเดอร์เป็นเสร็จสมบูรณ์แล้ว"));
    }
}