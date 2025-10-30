package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.OrderDetail;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.ProductImage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerOrderDetailDto {

    private Long id;
    private Timestamp orderDate;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentCode;
    private ShippingMethodInfo shippingMethod;
    private List<OrderDetailInfo> orderDetails;

    // Lớp nội bộ cho thông tin vận chuyển
    @Getter
    @Setter
    private static class ShippingMethodInfo {
        private String name;
        private BigDecimal fee;

        public ShippingMethodInfo(com.utegiftshop.entity.ShippingMethod sm) {
            if (sm != null) {
                this.name = sm.getName();
                this.fee = sm.getFee();
            }
        }
    }

    // Lớp nội bộ cho chi tiết sản phẩm trong đơn hàng
    @Getter
    @Setter
    private static class OrderDetailInfo {
        private int quantity;
        private BigDecimal price;
        private ProductInfo product;

        public OrderDetailInfo(OrderDetail detail) {
            this.quantity = detail.getQuantity();
            this.price = detail.getPrice();
            if (detail.getProduct() != null) {
                this.product = new ProductInfo(detail.getProduct());
            }
        }
    }

    // Lớp nội bộ cho thông tin sản phẩm
    @Getter
    @Setter
    private static class ProductInfo {
        private Long id;
        private String name;
        private List<ProductImageInfo> images;

        public ProductInfo(Product product) {
            this.id = product.getId();
            this.name = product.getName();
            if (product.getImages() != null) {
                this.images = product.getImages().stream()
                                    .map(ProductImageInfo::new)
                                    .collect(Collectors.toList());
            } else {
                this.images = Collections.emptyList();
            }
        }
    }
    
    // Lớp nội bộ cho hình ảnh sản phẩm
    @Getter
    @Setter
    private static class ProductImageInfo {
        private String imageUrl;

        public ProductImageInfo(ProductImage image) {
            this.imageUrl = image.getImageUrl();
        }
    }


    // Constructor chính
    public CustomerOrderDetailDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        this.shippingAddress = order.getShippingAddress();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.paymentCode = order.getPaymentCode();

        if (order.getShippingMethod() != null) {
            this.shippingMethod = new ShippingMethodInfo(order.getShippingMethod());
        }

        if (order.getOrderDetails() != null) {
            this.orderDetails = order.getOrderDetails().stream()
                                    .map(OrderDetailInfo::new)
                                    .collect(Collectors.toList());
        } else {
            this.orderDetails = Collections.emptyList();
        }
    }
}