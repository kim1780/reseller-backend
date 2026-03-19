package org.example.resellerbackend.dto;

import java.math.BigDecimal;

public class ShopProductResponse {
    public Long shopProductId;
    public Long productId;
    public String productName;
    public String category;
    public String imageUrl;
    public BigDecimal costPrice;
    public BigDecimal minPrice;
    public BigDecimal sellingPrice;
    public Integer stock;
    public String status;

    // คืนชีพ Constructor ให้มัน!
    public ShopProductResponse(Long spId, Long pId, String pName, String cat, String img, BigDecimal cost, BigDecimal min, BigDecimal sell, Integer st, String stat) {
        this.shopProductId = spId;
        this.productId = pId;
        this.productName = pName;
        this.category = cat;
        this.imageUrl = img;
        this.costPrice = cost;
        this.minPrice = min;
        this.sellingPrice = sell;
        this.stock = st;
        this.status = stat;
    }
}