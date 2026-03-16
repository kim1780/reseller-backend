package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // ไม่ต้องเขียนอะไรเพิ่มเช่นกัน (Long คือชนิดของ Primary Key ซึ่งก็คือ userId)
}