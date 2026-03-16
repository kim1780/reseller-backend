package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.WalletLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalletLogRepository extends JpaRepository<WalletLog, Long> {

    // ฟังก์ชันนี้สำคัญมาก! เอาไว้ดึงประวัติเงินเข้าออกของยูสเซอร์คนนั้นๆ
    // (เรียงจากล่าสุดไปเก่าสุดให้ด้วย หน้าเว็บจะได้โชว์สวยๆ)
    List<WalletLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // เอาไว้รองรับโค้ดเก่าใน Controller ที่ไม่ได้ใส่คำว่า OrderBy
    List<WalletLog> findByUserId(Long userId);
}