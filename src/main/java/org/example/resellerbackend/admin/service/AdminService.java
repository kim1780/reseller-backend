package org.example.resellerbackend.admin.service;

import org.example.resellerbackend.admin.dto.AddProductReq;
import org.example.resellerbackend.admin.dto.AdminDashboardRes;
import org.example.resellerbackend.admin.entity.*;
import org.example.resellerbackend.admin.repository.*;
import org.example.resellerbackend.entity.Orders;
import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.entity.Wallet;
import org.example.resellerbackend.entity.WalletLog;
import org.example.resellerbackend.repository.OrdersRepository;
import org.example.resellerbackend.repository.UserRepository;
import org.example.resellerbackend.repository.WalletRepository;
import org.example.resellerbackend.repository.WalletLogRepository;
import org.example.resellerbackend.repository.ShopRepository;
import org.example.resellerbackend.entity.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.resellerbackend.admin.dto.AdminDashboardFullRes;
import java.util.Comparator;
import java.util.Map;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
    private final AdminProductRepository productRepo;
    private final AdminOrderRepository orderRepo;
    private final AdminUserRepository userRepo;

    // ✅ ใช้ Repository เดียวกับ reseller (wallets table จริง)
    @Autowired private UserRepository userRepository;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletLogRepository walletLogRepository;
    @Autowired private ShopRepository shopRepository;

    public AdminService(AdminProductRepository p, AdminUserRepository u, AdminOrderRepository o) {
        this.productRepo = p;
        this.userRepo = u;
        this.orderRepo = o;
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

    // ✅ ดึงจาก ordersRepository จริง ไม่ใช่ adminOrderRepo
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void shipOrder(Long orderId) {
        // ✅ ใช้ ordersRepository จริง
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));

        if (!"pending".equals(order.getStatus())
                && !"รอดำเนินการ".equals(order.getStatus())
                && !"รอชำระเงิน".equals(order.getStatus())) {
            throw new RuntimeException("ออเดอร์นี้ไม่ได้รอดำเนินการ");
        }

        order.setStatus("shipped");
        ordersRepository.save(order);

        // ✅ หา userId จาก shop → หา wallet ด้วย userId (เหมือน reseller)
        Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
        if (shop == null) return;

        Long userId = shop.getUserId();
        BigDecimal profit = order.getResellerProfit() != null
                ? order.getResellerProfit() : BigDecimal.ZERO;

        // บันทึก WalletLog
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setOrderId(order.getId());
        log.setAmount(profit);
        log.setCreatedAt(java.time.LocalDateTime.now());
        walletLogRepository.save(log);

        // อัปเดต Wallet balance
        Wallet wallet = walletRepository.findById(userId).orElseGet(() -> {
            Wallet newW = new Wallet();
            newW.setUserId(userId);
            newW.setBalance(BigDecimal.ZERO);
            return walletRepository.save(newW);
        });

        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(current.add(profit));
        walletRepository.save(wallet);
    }

    public void completeOrder(Long orderId) {
        // ✅ ใช้ ordersRepository จริง
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));
        order.setStatus("completed");
        ordersRepository.save(order);
    }

    public AdminDashboardRes getDashboardStats() {
        AdminDashboardRes res = new AdminDashboardRes();

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

        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if ("reseller".equals(u.getRole())) {
                if ("approved".equals(u.getStatus())) res.setTotalResellers(res.getTotalResellers() + 1);
                if ("pending".equals(u.getStatus())) res.setPendingResellers(res.getPendingResellers() + 1);
            }
        }

        return res;

    }
    public AdminDashboardFullRes getFullDashboard() {
        AdminDashboardRes stats = getDashboardStats();
        List<Orders> orders = getAllOrders();

        List<User> resellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()))
                .sorted(Comparator.comparingLong(User::getId).reversed())
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> resellerList = resellers.stream().map(u -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("address", u.getAddress());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt());
            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopSlug", shop.getShopSlug());
                m.put("shopId", shop.getId());
            });
            return m;
        }).collect(java.util.stream.Collectors.toList());

        return new AdminDashboardFullRes(stats, orders, resellerList);
    }

    public Map<String, Object> getOrdersData() {
        List<Orders> orders = getAllOrders();

        List<User> resellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()))
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> resellerList = resellers.stream().map(u -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopId", shop.getId());
            });
            return m;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orderCount", orders.size());
        result.put("orders", orders);
        result.put("resellers", resellerList);
        return result;
    }

    // ✅ Pagination version ของ getOrdersData
    public Map<String, Object> getOrdersData(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage = ordersRepository.findAllByOrderByIdDesc(pageable);

        List<User> resellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()))
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> resellerList = resellers.stream().map(u -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopId", shop.getId());
            });
            return m;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orders", ordersPage.getContent());
        result.put("currentPage", ordersPage.getNumber());
        result.put("totalPages", ordersPage.getTotalPages());
        result.put("totalOrders", ordersPage.getTotalElements());
        result.put("pageSize", ordersPage.getSize());
        result.put("resellers", resellerList);
        return result;
    }

    // ✅ Pagination + Search สำหรับ /api/admin/orders/all
    public Map<String, Object> getOrdersPaginated(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage;

        if (search != null && !search.trim().isEmpty()) {
            ordersPage = ordersRepository.searchOrders(search.trim(), pageable);
        } else {
            ordersPage = ordersRepository.findAllByOrderByIdDesc(pageable);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orders", ordersPage.getContent());
        result.put("currentPage", ordersPage.getNumber());
        result.put("totalPages", ordersPage.getTotalPages());
        result.put("totalOrders", ordersPage.getTotalElements());
        result.put("pageSize", ordersPage.getSize());
        return result;
    }

}