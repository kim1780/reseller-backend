package org.example.resellerbackend.admin.controller;

import org.example.resellerbackend.entity.Shop;
import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.repository.ShopRepository;
import org.example.resellerbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/resellers")
public class AdminResellerController {

    // ✅ ใช้ UserRepository และ ShopRepository ตัวเดียวกับที่ Register ใช้
    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;

    // ดึง reseller ทั้งหมด พร้อมชื่อร้าน
    @GetMapping("/all")
    public ResponseEntity<?> getAllResellers() {
        List<User> resellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()))
                .sorted(Comparator.comparingLong(User::getId).reversed()) // ใหม่ขึ้นก่อน
                .collect(Collectors.toList());

        // รวม shopName เข้าไปด้วย
        List<Map<String, Object>> result = resellers.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("address", u.getAddress());
            m.put("status", u.getStatus());

            // ดึงชื่อร้านและ slug
            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopSlug", shop.getShopSlug());
                m.put("shopId", shop.getId());
            });

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // อนุมัติตัวแทน
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approveReseller(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("approved");
            userRepository.save(user);
            return ResponseEntity.ok("อนุมัติตัวแทนสำเร็จ");
        }).orElse(ResponseEntity.badRequest().body("ไม่พบตัวแทน ID " + id));
    }

    // ปฏิเสธตัวแทน
    @PutMapping("/{id}/reject")
    public ResponseEntity<String> rejectReseller(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("rejected");
            userRepository.save(user);
            return ResponseEntity.ok("ปฏิเสธตัวแทนสำเร็จ");
        }).orElse(ResponseEntity.badRequest().body("ไม่พบตัวแทน ID " + id));
    }
}