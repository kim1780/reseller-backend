package org.example.resellerbackend.dto;

public class CheckoutRequest {
    private Long shopProductId;
    private Integer quantity;
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    // กูเขียน Getter / Setter ให้ครบทุกตัวแล้วสัส ก๊อปไปวางทับได้เลย!
    public Long getShopProductId() {
        return shopProductId;
    }

    public void setShopProductId(Long shopProductId) {
        this.shopProductId = shopProductId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }
}