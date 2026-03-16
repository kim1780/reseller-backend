package org.example.resellerbackend.controller;

import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.repository.UserRepository;
import org.example.resellerbackend.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. หา User จาก Email
        return userRepository.findByEmail(request.getEmail()).map(user -> {

            // 2. เช็ครหัสผ่าน (BR-01, BR-18)
            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้องสัสคิม!");
            }

            // 3. เช็คสถานะการอนุมัติตัวแทน (BR-16, BR-17)
            if ("pending".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีรออนุมัติ กรุณารอการติดต่อ");
            }

            if ("rejected".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403).body("บัญชีนี้ไม่ได้รับการอนุมัติสัส!");
            }

            // 4. ถ้าผ่านสถานะ approved ให้เข้าสู่ระบบได้ (BR-15)
            return ResponseEntity.ok(user);

        }).orElse(ResponseEntity.status(401).body("ไม่พบผู้ใช้งานในระบบนะคิม"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logout สำเร็จแล้วสัส กลับบ้านไปนอนไป๊!");
    }
}