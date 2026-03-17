package org.example.resellerbackend.admin.controller;
import org.example.resellerbackend.admin.dto.AdminLoginReq;
import org.example.resellerbackend.admin.entity.AdminUserEntity;
import org.example.resellerbackend.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> adminLogin(@RequestBody AdminLoginReq req) {
        AdminUserEntity user = adminService.getUserByEmail(req.getEmail());

        if (user == null || !user.getPassword().equals(req.getPassword())) {
            return ResponseEntity.status(401).body("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
        }

        if (!"admin".equals(user.getRole())) {
            return ResponseEntity.status(403).body("ปฏิเสธการเข้าถึง แสดงหน้า Forbidden");
        }

        return ResponseEntity.ok("Login สำเร็จ Redirect ไป /admin/dashboard");
    }
}