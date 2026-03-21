package org.example.resellerbackend.admin.controller;

import org.example.resellerbackend.admin.service.AdminService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping
    public ResponseEntity<?> getFullDashboard(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        var data = adminService.getFullDashboard();

        String etag = "\"" + data.getStats().getTotalOrders() + "-" + data.getStats().getTotalResellers() + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                .body(data);
    }

    @GetMapping("/orders-data")
    public ResponseEntity<?> getOrdersData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        var data = adminService.getOrdersData(page, size);

        String etag = "\"orders-" + data.get("totalOrders") + "-p" + page + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                .body(data);
    }
}