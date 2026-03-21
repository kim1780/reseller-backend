package org.example.resellerbackend.controller;

import org.example.resellerbackend.entity.Orders;
import org.example.resellerbackend.entity.User;
import org.example.resellerbackend.repository.OrdersRepository;
import org.example.resellerbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/public")
public class PublicStatsController {

    @Autowired private UserRepository userRepository;
    @Autowired private OrdersRepository ordersRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getPublicStats() {
        Map<String, Object> res = new HashMap<>();

        // 1. Total Resellers (Approved)
        long totalResellers = userRepository.findAll().stream()
                .filter(u -> "reseller".equals(u.getRole()) && "approved".equals(u.getStatus()))
                .count();
        res.put("totalResellers", totalResellers);

        // 2. Total Orders & Sales
        List<Orders> allOrders = ordersRepository.findAll();
        long totalOrders = allOrders.size();
        BigDecimal totalSales = allOrders.stream()
                .filter(o -> !"pending".equalsIgnoreCase(o.getStatus())) // นับเฉพาะที่จ่ายแล้ว/ส่งแล้ว
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        res.put("totalOrders", totalOrders);
        res.put("totalSales", totalSales);

        // 3. Latest Order (Mock-like but from real data if available)
        Optional<Orders> latestOrderOpt = allOrders.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .findFirst();

        if (latestOrderOpt.isPresent()) {
            Orders latest = latestOrderOpt.get();
            Map<String, Object> lo = new HashMap<>();
            lo.put("orderNumber", latest.getOrderNumber());
            lo.put("customerName", latest.getCustomerName());
            lo.put("totalAmount", latest.getTotalAmount());
            lo.put("resellerProfit", latest.getResellerProfit());
            res.put("latestOrder", lo);
        } else {
            res.put("latestOrder", null);
        }

        return ResponseEntity.ok(res);
    }
}
