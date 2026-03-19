package org.example.resellerbackend.controller;

import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.entity.Shop;
import org.example.resellerbackend.repository.UserRepository;
import org.example.resellerbackend.repository.ShopRepository;
import org.example.resellerbackend.dto.LoginRequest;
import org.example.resellerbackend.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("อีเมลนี้ถูกใช้งานแล้ว!");
        }

        boolean shopExists = shopRepository.findAll().stream()
                .anyMatch(s -> s.getShopName().equalsIgnoreCase(request.getShopName()));
        if (shopExists) {
            return ResponseEntity.badRequest().body("ชื่อร้านนี้ถูกใช้แล้ว!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());
        user.setRole("reseller");
        user.setStatus("pending");
        user.setAddress(request.getAddress());
        User savedUser = userRepository.save(user);

        Shop shop = new Shop();
        shop.setUserId(savedUser.getId());
        shop.setShopName(request.getShopName());
        shop.setShopSlug(request.getShopName().toLowerCase().replace(" ", "-"));
        shopRepository.save(shop);

        return ResponseEntity.ok("สมัครสำเร็จ! รอ Admin อนุมัติสถานะ!");
    }

    // ✅ แก้แล้ว: ส่ง shopId และ shopSlug กลับมาด้วยตอน login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail()).map(user -> {
            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้อง!");
            }
            if ("pending".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีรออนุมัติ กรุณารอการติดต่อ");
            }
            if ("rejected".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีนี้ไม่ได้รับการอนุมัติ!");
            }

            // สร้าง response object รวม shopId ไว้ด้วย
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("status", user.getStatus());
            response.put("phone", user.getPhone());
            response.put("address", user.getAddress());

            // ดึง shopId และ shopSlug จาก Shop table
            Optional<Shop> shopOpt = shopRepository.findByUserId(user.getId());
            if (shopOpt.isPresent()) {
                Shop shop = shopOpt.get();
                response.put("shopId", shop.getId());
                response.put("shopSlug", shop.getShopSlug());
                response.put("shopName", shop.getShopName());
            }

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(401).body("ไม่พบผู้ใช้งานในระบบ"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logout สำเร็จ!");
    }
}