package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminWalletLogEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminWalletLogRepository extends CrudRepository<AdminWalletLogEntity, Long> {
}