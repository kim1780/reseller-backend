package org.example.resellerbackend.security;

import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migration: hash all plain-text passwords on startup (one-time).
 * จะตรวจสอบ password ที่ยังไม่ได้ hash แล้ว hash ให้อัตโนมัติ
 */
@Component
public class PasswordMigration implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigration(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        int count = 0;

        for (User user : users) {
            String pwd = user.getPassword();
            // BCrypt hash เริ่มต้นด้วย $2a$ หรือ $2b$
            // ถ้ายังไม่ใช่ hash ให้ทำการ hash
            if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(pwd));
                userRepository.save(user);
                count++;
            }
        }

        if (count > 0) {
            System.out.println("✅ Password Migration: hashed " + count + " plain-text passwords.");
        } else {
            System.out.println("✅ Password Migration: all passwords already hashed.");
        }
    }
}
