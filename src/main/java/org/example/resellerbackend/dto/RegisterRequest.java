package org.example.resellerbackend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String phone;      // เติมสัส!
    private String password;   // เติมสัส!
    private String shopName;   // เติมสัส! (เอาไปทำชื่อร้าน)
    private String address;    // เติมสัส!
    private String role;       // ปกติคือ "reseller"
}