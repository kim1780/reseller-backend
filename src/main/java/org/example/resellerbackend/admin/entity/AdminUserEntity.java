package org.example.resellerbackend.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime; // <--- เพิ่ม import นี้

@Entity
@Table(name = "users")
public class AdminUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String role;
    private String status;

    // --- ส่วนที่เพิ่มใหม่ (วันที่สมัคร) ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(); // สร้างวันที่อัตโนมัติเมื่อมีข้อมูลใหม่
    }

    // --- Getters and Setters (ของเดิม และเพิ่มตัวใหม่) ---
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // (Getter/Setter ตัวอื่นๆ ของเดิมคงไว้เหมือนเดิมครับ)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}