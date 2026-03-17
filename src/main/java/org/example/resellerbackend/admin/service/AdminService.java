package org.example.resellerbackend.admin.service;


import org.example.resellerbackend.admin.dto.AddProductReq;
import org.example.resellerbackend.admin.dto.AdminDashboardRes;
import org.example.resellerbackend.admin.entity.*;
import org.example.resellerbackend.admin.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    private final AdminProductRepository productRepo;
    private final AdminUserRepository userRepo;
    private final AdminOrderRepository orderRepo;
    private final AdminWalletRepository walletRepo;
    private final AdminWalletLogRepository walletLogRepo;

    public AdminService(AdminProductRepository p, AdminUserRepository u, AdminOrderRepository o, AdminWalletRepository w, AdminWalletLogRepository wl) {
        this.productRepo = p;
        this.userRepo = u;
        this.orderRepo = o;
        this.walletRepo = w;
        this.walletLogRepo = wl;
    }

    // ... (ระบบ Login และ addProduct เหมือนเดิม) ...
    public AdminUserEntity getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public void addProduct(AddProductReq req) {
        AdminProductEntity product = new AdminProductEntity();
        product.setName(req.getName());
        product.setCostPrice(req.getCostPrice());
        product.setMinPrice(req.getMinPrice());
        product.setStock(req.getStock());
        productRepo.save(product);
    }

    // --- แก้ไข getAllProducts ให้รองรับแค่การค้นหา ---
    public List<AdminProductEntity> getAllProducts(String search) {
        // ถ้ามีการพิมพ์คำค้นหาส่งมา
        if (search != null && !search.trim().isEmpty()) {
            return productRepo.findByNameContainingIgnoreCase(search);
        }

        // ถ้าไม่ได้ค้นหา ให้ดึงทั้งหมดออกมาเป็น List
        List<AdminProductEntity> list = new ArrayList<>();
        productRepo.findAll().forEach(list::add);
        return list;
    }

    public void editProduct(Long id, AddProductReq req) {
        AdminProductEntity product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบสินค้า"));

        product.setName(req.getName());
        product.setCostPrice(req.getCostPrice());
        product.setMinPrice(req.getMinPrice());
        product.setStock(req.getStock());
        productRepo.save(product);
    }

    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    // ... (ส่วน Resellers, Orders, Dashboard ของเดิมทั้งหมด ปล่อยไว้เหมือนเดิมครับ) ...
    public List<AdminUserEntity> getAllResellers() {
        List<AdminUserEntity> list = new ArrayList<>();
        userRepo.findAll().forEach(u -> {
            if ("reseller".equals(u.getRole())) list.add(u);
        });
        return list;
    }

    public void updateResellerStatus(Long id, String status) {
        AdminUserEntity u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบตัวแทน"));
        u.setStatus(status);
        userRepo.save(u);
    }

    public List<AdminOrderEntity> getAllOrders() {
        List<AdminOrderEntity> list = new ArrayList<>();
        orderRepo.findAll().forEach(list::add);
        return list;
    }

    @Transactional
    public void shipOrder(Long orderId) {
        AdminOrderEntity order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));
        if (!"pending".equals(order.getStatus())) throw new RuntimeException("ออเดอร์นี้ไม่ได้รอดำเนินการ");

        order.setStatus("shipped");
        orderRepo.save(order);

        Long resellerUserId = order.getShopId();
        AdminWalletEntity wallet = walletRepo.findByUserId(resellerUserId);

        if (wallet != null) {
            wallet.setBalance(wallet.getBalance().add(order.getResellerProfit()));
            walletRepo.save(wallet);

            AdminWalletLogEntity log = new AdminWalletLogEntity();
            log.setWalletId(wallet.getId());
            log.setAmount(order.getResellerProfit());
            walletLogRepo.save(log);
        }
    }

    public void completeOrder(Long orderId) {
        AdminOrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));
        order.setStatus("completed");
        orderRepo.save(order);
    }

    public AdminDashboardRes getDashboardStats() {
        AdminDashboardRes res = new AdminDashboardRes();
        orderRepo.findAll().forEach(o -> {
            res.setTotalOrders(res.getTotalOrders() + 1);
            if ("pending".equals(o.getStatus())) {
                res.setPendingOrders(res.getPendingOrders() + 1);
            }
            if ("shipped".equals(o.getStatus()) || "completed".equals(o.getStatus())) {
                res.setTotalSales(res.getTotalSales().add(o.getTotalAmount()));
                res.setTotalProfit(res.getTotalProfit().add(o.getResellerProfit()));
            }
        });
        userRepo.findAll().forEach(u -> {
            if ("reseller".equals(u.getRole())) {
                if ("approved".equals(u.getStatus())) {
                    res.setTotalResellers(res.getTotalResellers() + 1);
                }
                if ("pending".equals(u.getStatus())) {
                    res.setPendingResellers(res.getPendingResellers() + 1);
                }
            }
        });
        return res;
    }
}