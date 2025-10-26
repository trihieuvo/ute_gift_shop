package com.utegiftshop.controller;

import com.utegiftshop.dto.request.CheckoutRequest;
import com.utegiftshop.entity.*;
import com.utegiftshop.repository.*;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductRepository productRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        Long userId = getCurrentUser().getId();
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("Giỏ hàng trống.");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ."));

        // Create new Order
        Order order = new Order();
        User user = new User();
        user.setId(userId);
        order.setUser(user);
        order.setShippingAddress(address.getFullAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("NEW"); // Trạng thái ban đầu

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ số lượng.");
            }
            // Create OrderDetail
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice()); // Lưu giá tại thời điểm mua
            orderDetails.add(detail);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));

            // Update product stock
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);

        // Save Order and OrderDetails
        Order savedOrder = orderRepository.save(order);
        for(OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);
        }

        // Clear cart
        cartItemRepository.deleteByUserId(userId);

        return ResponseEntity.ok(savedOrder);
    }
}