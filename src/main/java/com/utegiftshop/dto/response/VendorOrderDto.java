package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Order;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VendorOrderDto {
    
    private Long orderId;
    private Timestamp orderDate;
    private String status;
    private String paymentMethod;
    private String shippingAddress;
    private String deliveryNote;
    
    // Customer info
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Vendor-specific info
    private BigDecimal vendorTotalAmount;
    private Integer vendorItemCount;
    private List<VendorOrderDetailDto> vendorItems;
    
    /**
     * Constructor tạo DTO từ Order và list items của vendor
     */
    public VendorOrderDto(Order order, List<VendorOrderDetailDto> vendorItems) {
        this.orderId = order.getId();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.shippingAddress = order.getShippingAddress();
        this.deliveryNote = order.getDeliveryNote();
        
        // Customer info
        if (order.getUser() != null) {
            this.customerName = order.getUser().getFullName();
            this.customerEmail = order.getUser().getEmail();
            this.customerPhone = order.getUser().getPhoneNumber();
        }
        
        // Vendor items
        this.vendorItems = vendorItems;
        this.vendorItemCount = vendorItems != null ? vendorItems.size() : 0;
        
        // Calculate vendor total
        this.vendorTotalAmount = vendorItems != null 
            ? vendorItems.stream()
                .map(VendorOrderDetailDto::getTotalItemPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;
    }
}