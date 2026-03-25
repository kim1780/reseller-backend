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

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;

    // จัดการตัวเเทน
    @GetMapping("/all")
    public ResponseEntity<?> getAllResellers() {
        List<User> resellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()))
                .sorted(Comparator.comparingLong(User::getId).reversed())
                .collect(Collectors.toList());

        List<Map<String, Object>> result = resellers.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("address", u.getAddress());
            m.put("status", u.getStatus());
            // ✅ เพิ่ม createdAt
            m.put("createdAt", u.getCreatedAt());

            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopSlug", shop.getShopSlug());
                m.put("shopId", shop.getId());
            });

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    //ยืนยันตัวเเทน
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReseller(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("approved");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "อนุมัติตัวแทนสำเร็จ"));
        }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่พบตัวแทน ID " + id)));
    }

    //ปฏิเสธตัวแทน
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReseller(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("rejected");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "ปฏิเสธตัวแทนสำเร็จ"));
        }).orElse(ResponseEntity.badRequest().body(Map.of("message", "ไม่พบตัวแทน ID " + id)));
    }
}