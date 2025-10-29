package com.utegiftshop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutRequest {
    private Long addressId;
    private String paymentMethod; // "COD", "VNPAY", etc.
    private Integer shippingMethodId;
}