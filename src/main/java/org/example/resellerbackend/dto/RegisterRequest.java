package org.example.resellerbackend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String phone;      // เติม!
    private String password;   // เติม!
    private String shopName;   // เติม! (เอาไปทำชื่อร้าน)
    private String address;    // เติม!
    private String role;       // ปกติคือ "reseller"
}