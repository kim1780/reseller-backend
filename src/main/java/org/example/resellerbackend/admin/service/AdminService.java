package org.example.resellerbackend.admin.service;

import org.example.resellerbackend.admin.dto.AddProductReq;
import org.example.resellerbackend.admin.dto.AdminDashboardRes;
import org.example.resellerbackend.admin.entity.*;
import org.example.resellerbackend.admin.repository.*;
import org.example.resellerbackend.entity.Orders;
import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.repository.OrdersRepository;
import org.example.resellerbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    private final AdminProductRepository productRepo;
    private final AdminOrderRepository orderRepo;
    private final AdminWalletRepository walletRepo;
    private final AdminWalletLogRepository walletLogRepo;

    // ✅ ใช้ Repository จริงที่ Register/Checkout ใช้
    @Autowired private UserRepository userRepository;
    @Autowired private OrdersRepository ordersRepository;

    // ยังคง AdminUserRepository ไว้สำหรับ legacy methods ที่ยังใช้อยู่
    private final AdminUserRepository userRepo;

    public AdminService(AdminProductRepository p, AdminUserRepository u,
                        AdminOrderRepository o, AdminWalletRepository w,
                        AdminWalletLogRepository wl) {
        this.productRepo = p;
        this.userRepo = u;
        this.orderRepo = o;
        this.walletRepo = w;
        this.walletLogRepo = wl;
    }

    public AdminUserEntity getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public void addProduct(AddProductReq req) {
        AdminProductEntity product = new AdminProductEntity();
        product.setName(req.getName());
        product.setCostPrice(req.getCostPrice());
        product.setMinPrice(req.getMinPrice());
        product.setStock(req.getStock());
        product.setImageUrl(req.getImageUrl());
        productRepo.save(product);
    }

    public List<AdminProductEntity> getAllProducts(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return productRepo.findByNameContainingIgnoreCase(search);
        }
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
        product.setImageUrl(req.getImageUrl());
        productRepo.save(product);
    }

    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    // ยังคงไว้สำหรับ backward compat (ไม่ใช้แล้วหลัง AdminResellerController แก้)
    public List<AdminUserEntity> getAllResellers() {
        List<AdminUserEntity> list = new ArrayList<>();
        userRepo.findAll().forEach(u -> {
            if ("reseller".equals(u.getRole())) list.add(u);
        });
        return list;
    }

    public void updateResellerStatus(Long id, String status) {
        AdminUserEntity u = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบตัวแทน"));
        u.setStatus(status);
        userRepo.save(u);
    }

    public List<AdminOrderEntity> getAllOrders() {
        return orderRepo.findTop100ByOrderByIdDesc();
    }

    @Transactional
    public void shipOrder(Long orderId) {
        AdminOrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));

        if (!"pending".equals(order.getStatus()) && !"รอดำเนินการ".equals(order.getStatus())) {
            throw new RuntimeException("ออเดอร์นี้ไม่ได้รอดำเนินการ");
        }

        order.setStatus("shipped");
        orderRepo.save(order);

        AdminWalletEntity wallet = walletRepo.findByShopId(order.getShopId());
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

    // ✅ แก้แล้ว: ดึงจาก ordersRepository และ userRepository จริง
    public AdminDashboardRes getDashboardStats() {
        AdminDashboardRes res = new AdminDashboardRes();

        // นับออเดอร์จาก orders table จริง
        List<Orders> allOrders = ordersRepository.findAll();
        for (Orders o : allOrders) {
            res.setTotalOrders(res.getTotalOrders() + 1);

            String status = o.getStatus();
            if ("pending".equals(status) || "รอดำเนินการ".equals(status) || "รอชำระเงิน".equals(status)) {
                res.setPendingOrders(res.getPendingOrders() + 1);
            }
            if ("shipped".equals(status) || "completed".equals(status) || "จัดส่งแล้ว".equals(status)) {
                BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal profit = o.getResellerProfit() != null ? o.getResellerProfit() : BigDecimal.ZERO;
                res.setTotalSales(res.getTotalSales().add(amount));
                res.setTotalProfit(res.getTotalProfit().add(profit));
            }
        }

        // นับ reseller จาก users table จริง
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if ("reseller".equals(u.getRole())) {
                if ("approved".equals(u.getStatus())) {
                    res.setTotalResellers(res.getTotalResellers() + 1);
                }
                if ("pending".equals(u.getStatus())) {
                    res.setPendingResellers(res.getPendingResellers() + 1);
                }
            }
        }

        return res;
    }
}