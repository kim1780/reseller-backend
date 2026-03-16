package org.example.resellerbackend.repository;

import org.example.resellerbackend.entity.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopProductRepository extends JpaRepository<ShopProduct, Long> {
    List<ShopProduct> findByShopId(Long shopId);
}