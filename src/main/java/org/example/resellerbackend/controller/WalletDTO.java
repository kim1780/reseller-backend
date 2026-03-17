package org.example.resellerbackend.controller;

import org.example.resellerbackend.entity.WalletLog;

import java.math.BigDecimal;
import java.util.List;

// ================= [DTO Classes] =================
class WalletDTO {
    public BigDecimal totalBalance;
    public List<WalletLog> history;

    public WalletDTO(BigDecimal b, List<WalletLog> h) {
        this.totalBalance = b;
        this.history = h;
    }
}
