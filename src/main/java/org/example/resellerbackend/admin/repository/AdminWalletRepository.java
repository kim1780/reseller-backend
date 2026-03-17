package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminWalletEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminWalletRepository extends CrudRepository<AdminWalletEntity, Long> {

    // คำสั่งพิเศษสำหรับค้นหากระเป๋าเงินจาก ID ของตัวแทน
    AdminWalletEntity findByUserId(Long userId);
}