package org.example.resellerbackend.admin.repository;


import org.example.resellerbackend.admin.entity.AdminOrderEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminOrderRepository extends CrudRepository<AdminOrderEntity, Long> {
    List<AdminOrderEntity> findTop100ByOrderByIdDesc();
}