package org.example.resellerbackend.admin.dto;

import java.util.List;
import java.util.Map;

public class AdminDashboardFullRes {
    private AdminDashboardRes stats;
    private List<?> orders;
    private List<Map<String, Object>> resellers;

    public AdminDashboardFullRes(AdminDashboardRes stats, List<?> orders, List<Map<String, Object>> resellers) {
        this.stats = stats;
        this.orders = orders;
        this.resellers = resellers;
    }

    public AdminDashboardRes getStats() { return stats; }
    public List<?> getOrders() { return orders; }
    public List<Map<String, Object>> getResellers() { return resellers; }


}