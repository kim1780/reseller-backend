package org.example.resellerbackend.dto;

import java.math.BigDecimal;

public class CustomerProductResponse {
    private Long id;
    private Long productId;
    private Long shopId;
    private BigDecimal sellingPrice;
    private Integer stock;
    private ProductDetail product;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public ProductDetail getProduct() { return product; }
    public void setProduct(ProductDetail product) { this.product = product; }

    public static class ProductDetail {
        private String productName;
        private String productImage;
        private Integer stock;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductImage() { return productImage; }
        public void setProductImage(String productImage) { this.productImage = productImage; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }
}