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

/**
 * Service สำหรับจัดการฝั่ง Admin
 * ครอบคลุม: สินค้า, ตัวแทน (reseller), ออเดอร์, กระเป๋าเงิน และ Dashboard
 */
@Service
public class AdminService {

    private final AdminProductRepository productRepo;
    private final AdminOrderRepository orderRepo;
    private final AdminUserRepository userRepo;

    // Repository ที่ใช้ร่วมกับฝั่ง reseller (ตาราง wallets, orders, users จริง)
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

    // ─────────────────────────────────────────────
    // USER
    // ─────────────────────────────────────────────

    /**
     * ค้นหา admin user จาก email
     *
     * @param email อีเมลที่ต้องการค้นหา
     * @return AdminUserEntity หรือ null ถ้าไม่พบ
     */
    public AdminUserEntity getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // ─────────────────────────────────────────────
    // PRODUCT
    // ─────────────────────────────────────────────

    /**
     * เพิ่มสินค้าใหม่เข้าระบบ
     *
     * @param req ข้อมูลสินค้าที่ต้องการเพิ่ม
     */
    public void addProduct(AddProductReq req) {
        AdminProductEntity product = new AdminProductEntity();
        product.setName(req.getName());
        product.setCostPrice(req.getCostPrice());
        product.setMinPrice(req.getMinPrice());
        product.setStock(req.getStock());
        product.setImageUrl(req.getImageUrl());
        productRepo.save(product);
    }

    /**
     * ดึงรายการสินค้าทั้งหมด รองรับการค้นหาด้วยชื่อ
     *
     * @param search คำค้นหา (ถ้าเป็น null หรือว่างจะดึงทั้งหมด)
     * @return รายการสินค้า
     */
    public List<AdminProductEntity> getAllProducts(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return productRepo.findByNameContainingIgnoreCase(search);
        }
        List<AdminProductEntity> list = new ArrayList<>();
        productRepo.findAll().forEach(list::add);
        return list;
    }

    /**
     * แก้ไขข้อมูลสินค้า
     *
     * @param id  ID ของสินค้าที่ต้องการแก้ไข
     * @param req ข้อมูลใหม่ที่ต้องการอัปเดต
     * @throws RuntimeException ถ้าไม่พบสินค้า
     */
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

    /**
     * ลบสินค้าออกจากระบบ
     *
     * @param id ID ของสินค้าที่ต้องการลบ
     */
    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    // ─────────────────────────────────────────────
    // RESELLER
    // ─────────────────────────────────────────────

    /**
     * ดึงรายชื่อตัวแทน (reseller) ทั้งหมด
     *
     * @return รายการ user ที่มี role = "reseller"
     */
    public List<AdminUserEntity> getAllResellers() {
        List<AdminUserEntity> list = new ArrayList<>();
        userRepo.findAll().forEach(u -> {
            if ("reseller".equals(u.getRole())) list.add(u);
        });
        return list;
    }

    /**
     * อัปเดตสถานะของตัวแทน เช่น approved / rejected / pending
     *
     * @param id     ID ของตัวแทนที่ต้องการแก้ไข
     * @param status สถานะใหม่
     * @throws RuntimeException ถ้าไม่พบตัวแทน
     */
    public void updateResellerStatus(Long id, String status) {
        AdminUserEntity u = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบตัวแทน"));
        u.setStatus(status);
        userRepo.save(u);
    }

    // ─────────────────────────────────────────────
    // ORDER
    // ─────────────────────────────────────────────

    /**
     * ดึงรายการออเดอร์ล่าสุด 100 รายการ เรียงจากใหม่ไปเก่า
     *
     * @return รายการออเดอร์
     */
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * จัดส่งออเดอร์ (เปลี่ยนสถานะเป็น "shipped")
     * พร้อมบันทึกกำไรเข้ากระเป๋าเงินของตัวแทนเจ้าของร้าน
     *
     * <p>ขั้นตอน:
     * <ol>
     *   <li>ตรวจสอบว่าออเดอร์อยู่ในสถานะรอดำเนินการ</li>
     *   <li>เปลี่ยนสถานะออเดอร์เป็น "shipped"</li>
     *   <li>หา userId จาก shopId ของออเดอร์</li>
     *   <li>บันทึก WalletLog และอัปเดตยอดกระเป๋าเงิน</li>
     * </ol>
     *
     * @param orderId ID ของออเดอร์ที่ต้องการจัดส่ง
     * @throws RuntimeException ถ้าไม่พบออเดอร์ หรือสถานะไม่ถูกต้อง
     */
    @Transactional
    public void shipOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));

        // ตรวจสอบว่าออเดอร์อยู่ในสถานะที่จัดส่งได้
        if (!"pending".equals(order.getStatus())
                && !"รอดำเนินการ".equals(order.getStatus())
                && !"รอชำระเงิน".equals(order.getStatus())) {
            throw new RuntimeException("ออเดอร์นี้ไม่ได้รอดำเนินการ");
        }

        order.setStatus("shipped");
        ordersRepository.save(order);

        // หาข้อมูลร้านค้าเพื่อระบุเจ้าของ (userId)
        Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
        if (shop == null) return;

        Long userId = shop.getUserId();
        BigDecimal profit = order.getResellerProfit() != null
                ? order.getResellerProfit() : BigDecimal.ZERO;

        // บันทึก log การได้รับกำไรของตัวแทน
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setOrderId(order.getId());
        log.setAmount(profit);
        log.setCreatedAt(java.time.LocalDateTime.now());
        walletLogRepository.save(log);

        // อัปเดตยอดกระเป๋าเงิน (สร้างใหม่ถ้ายังไม่มี)
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

    /**
     * ปิดออเดอร์ (เปลี่ยนสถานะเป็น "completed")
     *
     * @param orderId ID ของออเดอร์ที่ต้องการปิด
     * @throws RuntimeException ถ้าไม่พบออเดอร์
     */
    public void completeOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ไม่พบออเดอร์"));
        order.setStatus("completed");
        ordersRepository.save(order);
    }

    // ─────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────

    /**
     * คำนวณสถิติสรุปสำหรับ Dashboard
     * ได้แก่: จำนวนออเดอร์ทั้งหมด, ออเดอร์รอดำเนินการ,
     * ยอดขายรวม, กำไรรวม, จำนวนตัวแทนที่อนุมัติแล้ว/รออนุมัติ
     *
     * @return AdminDashboardRes ที่มีข้อมูลสถิติ
     */
    public AdminDashboardRes getDashboardStats() {
        AdminDashboardRes res = new AdminDashboardRes();

        // วนนับสถิติจากออเดอร์ทั้งหมด
        List<Orders> allOrders = ordersRepository.findAll();
        for (Orders o : allOrders) {
            res.setTotalOrders(res.getTotalOrders() + 1);
            String status = o.getStatus();

            // นับออเดอร์ที่รอดำเนินการ
            if ("pending".equals(status) || "รอดำเนินการ".equals(status) || "รอชำระเงิน".equals(status)) {
                res.setPendingOrders(res.getPendingOrders() + 1);
            }

            // รวมยอดขายและกำไรจากออเดอร์ที่จัดส่งแล้วหรือเสร็จสิ้น
            if ("shipped".equals(status) || "completed".equals(status) || "จัดส่งแล้ว".equals(status)) {
                BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal profit = o.getResellerProfit() != null ? o.getResellerProfit() : BigDecimal.ZERO;
                res.setTotalSales(res.getTotalSales().add(amount));
                res.setTotalProfit(res.getTotalProfit().add(profit));
            }
        }

        // วนนับตัวแทนตามสถานะ
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if ("reseller".equals(u.getRole())) {
                if ("approved".equals(u.getStatus())) res.setTotalResellers(res.getTotalResellers() + 1);
                if ("pending".equals(u.getStatus())) res.setPendingResellers(res.getPendingResellers() + 1);
            }
        }

        return res;
    }

    /**
     * ดึงข้อมูล Dashboard แบบเต็ม รวมสถิติ + ออเดอร์ล่าสุด + รายชื่อตัวแทน
     * ตัวแทนแต่ละคนจะมีข้อมูลร้านค้า (shopName, shopSlug, shopId) แนบมาด้วย
     *
     * @return AdminDashboardFullRes ที่รวมข้อมูลครบทุกส่วน
     */
    public AdminDashboardFullRes getFullDashboard() {
        AdminDashboardRes stats = getDashboardStats();
        List<Orders> orders = getAllOrders();

        // ดึงรายชื่อตัวแทนเรียงจากใหม่ไปเก่า พร้อมข้อมูลร้านค้า
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
            // แนบข้อมูลร้านค้าถ้ามี
            shopRepository.findByUserId(u.getId()).ifPresent(shop -> {
                m.put("shopName", shop.getShopName());
                m.put("shopSlug", shop.getShopSlug());
                m.put("shopId", shop.getId());
            });
            return m;
        }).collect(java.util.stream.Collectors.toList());

        return new AdminDashboardFullRes(stats, orders, resellerList);
    }

    /**
     * ดึงข้อมูลออเดอร์ทั้งหมดพร้อมรายชื่อตัวแทน (ไม่มี pagination)
     * ใช้สำหรับ endpoint ที่ต้องการข้อมูลออเดอร์ + ตัวแทนพร้อมกัน
     *
     * @return Map ที่มี key: orderCount, orders, resellers
     */
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

    /**
     * ดึงข้อมูลออเดอร์แบบ Pagination พร้อมรายชื่อตัวแทน
     *
     * @param page หน้าที่ต้องการ (เริ่มที่ 0)
     * @param size จำนวนรายการต่อหน้า
     * @return Map ที่มี key: orders, currentPage, totalPages, totalOrders, pageSize, resellers
     */
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

    /**
     * ดึงข้อมูลออเดอร์แบบ Pagination + ค้นหา
     * ใช้สำหรับ endpoint /api/admin/orders/all
     *
     * @param page   หน้าที่ต้องการ (เริ่มที่ 0)
     * @param size   จำนวนรายการต่อหน้า
     * @param search คำค้นหา (ถ้าเป็น null หรือว่างจะดึงทั้งหมด)
     * @return Map ที่มี key: orders, currentPage, totalPages, totalOrders, pageSize
     */
    public Map<String, Object> getOrdersPaginated(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage;

        if (search != null && !search.trim().isEmpty()) {
            // ค้นหาออเดอร์ตาม keyword
            ordersPage = ordersRepository.searchOrders(search.trim(), pageable);
        } else {
            // ดึงทั้งหมดเรียงจากใหม่ไปเก่า
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