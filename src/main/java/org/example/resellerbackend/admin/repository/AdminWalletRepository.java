package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminWalletEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminWalletRepository extends CrudRepository<AdminWalletEntity, Long> {

    // คำสั่งพิเศษสำหรับค้นหากระเป๋าเงินจาก ID ของตัวแทน
    AdminWalletEntity findByUserId(Long userId);
    @Query(value = "SELECT w.* FROM wallets w JOIN shops s ON s.user_id = w.user_id WHERE s.id = :shopId", nativeQuery = true)
    AdminWalletEntity findByShopId(@Param("shopId") Long shopId);
}