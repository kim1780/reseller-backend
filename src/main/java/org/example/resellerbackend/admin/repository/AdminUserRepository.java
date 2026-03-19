package org.example.resellerbackend.admin.repository;

import org.example.resellerbackend.admin.entity.AdminUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminUserRepository extends CrudRepository<AdminUserEntity, Long> {
    AdminUserEntity findByEmail(String email);
}