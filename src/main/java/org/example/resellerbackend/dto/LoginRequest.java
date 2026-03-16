package org.example.resellerbackend.dto;

import lombok.Data;

@Data // คิม! ตัวนี้แหละที่จะสร้าง getEmail() กับ getPassword() ให้มึงอัตโนมัติ
public class LoginRequest {
    private String email;
    private String password;
}