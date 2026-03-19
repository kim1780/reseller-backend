package org.example.resellerbackend.dto;

import java.util.List;

public class CheckoutRequest {

    public static class CartItem {
        private Long shopProductId;
        private Integer quantity;

        public Long getShopProductId() { return shopProductId; }
        public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    private List<CartItem> items;
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
}