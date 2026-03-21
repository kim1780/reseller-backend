package org.example.resellerbackend.admin.controller;


import jakarta.validation.Valid; // <--- อย่าลืม import ตัวนี้
import org.example.resellerbackend.admin.dto.AddProductReq;
import org.example.resellerbackend.admin.entity.AdminProductEntity;
import org.example.resellerbackend.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminService adminService;

    public AdminProductController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@Valid @RequestBody AddProductReq req) {
        if (req.getMinPrice().compareTo(req.getCostPrice()) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "ราคาขั้นต่ำต้องมากกว่าหรือเท่ากับราคาทุน"));
        }
        adminService.addProduct(req);
        return ResponseEntity.ok(Map.of("message", "เพิ่มสินค้าเรียบร้อยแล้ว"));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(name = "search", required = false) String search
    ) {
        List<AdminProductEntity> products = adminService.getAllProducts(search);
        if (products.isEmpty()) {
            return ResponseEntity.status(404).body("ไม่พบสินค้าที่คุณค้นหา");
        }
        return ResponseEntity.ok(products);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editProduct(@PathVariable Long id, @Valid @RequestBody AddProductReq req) {
        if (req.getMinPrice().compareTo(req.getCostPrice()) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "ราคาขั้นต่ำต้องมากกว่าหรือเท่ากับราคาทุน"));
        }
        try {
            adminService.editProduct(id, req);
            return ResponseEntity.ok(Map.of("message", "แก้ไขสินค้าเรียบร้อยแล้ว"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            adminService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", "ลบสินค้าเรียบร้อยแล้ว"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "ไม่สามารถลบสินค้าได้ เนื่องจากมีออเดอร์ค้างอยู่"));
        }
    }
}