package org.example.resellerbackend.admin.controller;
import org.example.resellerbackend.admin.dto.AdminLoginReq;
import org.example.resellerbackend.admin.entity.AdminUserEntity;
import org.example.resellerbackend.admin.service.AdminService;
import org.example.resellerbackend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(AdminService adminService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginReq req) {
        AdminUserEntity user = adminService.getUserByEmail(req.getEmail());

        if (user == null) {
            return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
        }

        String rawPassword = req.getPassword();
        String storedPassword = user.getPassword();
        boolean passwordOk = false;

        // 1. ลอง BCrypt ก่อน
        try {
            if (storedPassword != null && storedPassword.startsWith("$2a$")) {
                passwordOk = passwordEncoder.matches(rawPassword, storedPassword);
            }
        } catch (Exception ignored) {}

        // 2. ถ้า BCrypt ไม่ผ่าน ลอง plain-text
        if (!passwordOk && rawPassword.equals(storedPassword)) {
            passwordOk = true;
        }

        if (!passwordOk) {
            return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
        }

        if (!"admin".equals(user.getRole())) {
            return ResponseEntity.status(403).body("ปฏิเสธการเข้าถึง แสดงหน้า Forbidden");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("status", user.getStatus());
        response.put("name", "Administrator");

        return ResponseEntity.ok(response);
    }
}