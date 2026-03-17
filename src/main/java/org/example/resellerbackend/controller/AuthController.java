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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository; // เติมอันนี้ด้วยสัสคิม!

    // --- ภารกิจที่ 2: ระบบสมัครสมาชิก (มึงก๊อปส่วนนี้เพิ่มเข้าไป!) ---
    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // [BR-13] เช็ค Email ซ้ำ
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("อีเมลนี้ถูกใช้งานแล้วสัส!");
        }

        // [BR-14] เช็คชื่อร้านซ้ำ (ดักไว้ก่อนทำ URL)
        boolean shopExists = shopRepository.findAll().stream()
                .anyMatch(s -> s.getShopName().equalsIgnoreCase(request.getShopName()));
        if (shopExists) {
            return ResponseEntity.badRequest().body("ชื่อร้านนี้ถูกใช้แล้วสัส!");
        }

        // 1. บันทึก User (BR-12: สถานะ pending)
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword()); // รหัสผ่านดิบๆ ไปก่อนสัส
        user.setRole("reseller");
        user.setStatus("pending"); // ต้องรออนุมัติสัสคิม!
        user.setAddress(request.getAddress());
        User savedUser = userRepository.save(user);

        // 2. บันทึก Shop (ภารกิจที่ 4: ลิงก์หน้าร้านส่วนตัว)
        Shop shop = new Shop();
        shop.setUserId(savedUser.getId());
        shop.setShopName(request.getShopName());
        shop.setShopSlug(request.getShopName().toLowerCase().replace(" ", "-")); // ทำ URL ร้าน
        shopRepository.save(shop);

        return ResponseEntity.ok("สมัครสำเร็จ! รอ Admin อนุมัติสถานะสัส!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail()).map(user -> {
            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้องสัสคิม!");
            }
            if ("pending".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีรออนุมัติ กรุณารอการติดต่อ");
            }
            if ("rejected".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีนี้ไม่ได้รับการอนุมัติสัส!");
            }
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.status(401).body("ไม่พบผู้ใช้งานในระบบนะคิม"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logout สำเร็จแล้วสัส กลับบ้านไปนอนไป๊!");
    }
}