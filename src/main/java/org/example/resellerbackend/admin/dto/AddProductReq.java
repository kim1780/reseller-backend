package org.example.resellerbackend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AddProductReq {

    @NotBlank(message = "กรุณากรอกชื่อสินค้า")
    private String name;

    @NotNull(message = "กรุณากรอกราคาทุน")
    @Min(value = 0, message = "ราคาทุนต้องไม่ติดลบ")
    private BigDecimal costPrice;

    @NotNull(message = "กรุณากรอกราคาขั้นต่ำ")
    @Min(value = 0, message = "ราคาขั้นต่ำต้องไม่ติดลบ")
    private BigDecimal minPrice;

    @NotNull(message = "กรุณากรอกจำนวนสต๊อก")
    @Min(value = 0, message = "สต๊อกสินค้าต้องไม่ติดลบ")
    private Integer stock;

    private String imageUrl;

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}