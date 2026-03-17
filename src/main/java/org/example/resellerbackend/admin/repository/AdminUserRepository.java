package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminUserRepository extends CrudRepository<AdminUserEntity, Long> {

    // เพิ่มบรรทัดนี้เข้ามาครับ เพื่อให้หา User ด้วย Email ได้
    AdminUserEntity findByEmail(String email);
}