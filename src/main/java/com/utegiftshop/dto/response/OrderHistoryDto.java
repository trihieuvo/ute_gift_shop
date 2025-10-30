package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.OrderDetail;
import com.utegiftshop.entity.Product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderHistoryDto {
    private Long id;
    private Timestamp orderDate;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentCode;
    private List<OrderDetailDto> orderDetails;

    // Inner DTO for OrderDetail to safely get product ID
    @Getter
    @Setter
    public static class OrderDetailDto {
        private ProductDto product;

        public OrderDetailDto(OrderDetail orderDetail) {
            if (orderDetail.getProduct() != null) {
                this.product = new ProductDto(orderDetail.getProduct());
            }
        }
    }

    // Inner DTO for Product to safely get just the ID
    @Getter
    @Setter
    public static class ProductDto {
        private Long id;

        public ProductDto(Product product) {
            this.id = product.getId();
        }
    }


    public OrderHistoryDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        this.shippingAddress = order.getShippingAddress();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.paymentCode = order.getPaymentCode();
        if (order.getOrderDetails() != null) {
            this.orderDetails = order.getOrderDetails().stream()
                                    .map(OrderDetailDto::new)
                                    .collect(Collectors.toList());
        } else {
            this.orderDetails = Collections.emptyList();
        }
    }
}