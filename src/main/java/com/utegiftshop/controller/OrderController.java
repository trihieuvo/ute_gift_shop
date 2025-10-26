package com.utegiftshop.controller;

import com.utegiftshop.dto.request.CheckoutRequest;
import com.utegiftshop.entity.*;
import com.utegiftshop.repository.*;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus; 
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; 

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
        order.setUser(user); // Gán người dùng đặt hàng

        // === HOÀN TÁC: Chỉ lưu địa chỉ, không lưu SĐT/Tên (vì CSDL gốc không có) ===
        order.setShippingAddress(address.getFullAddress());
        // order.setRecipientName(address.getRecipientName()); // <-- XÓA DÒNG NÀY
        // order.setRecipientPhone(address.getPhoneNumber()); // <-- XÓA DÒNG NÀY
        // === KẾT THÚC HOÀN TÁC ===

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
        
        List<OrderDetail> savedDetails = new ArrayList<>();
        for(OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            savedDetails.add(orderDetailRepository.save(detail));
        }
        savedOrder.setOrderDetails(savedDetails); 

        // Clear cart
        cartItemRepository.deleteByUserId(userId);

        return ResponseEntity.ok(savedOrder);
    }

    // === CÁC API CỦA CUSTOMER (GIỮ NGUYÊN) ===
    @GetMapping("/my-history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        Long userId = getCurrentUser().getId();
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Optional<Order> orderOpt = orderRepository.findByIdAndUserId(orderId, userId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body("Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");
        }
        
        Order order = orderOpt.get();
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Optional<Order> orderOpt = orderRepository.findByIdAndUserId(orderId, userId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body("Không tìm thấy đơn hàng hoặc bạn không có quyền thao tác.");
        }

        Order order = orderOpt.get();
        if (!order.getStatus().equals("NEW")) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng ở trạng thái 'Mới' (NEW).");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        List<OrderDetail> details = orderDetailRepository.findAllById(
             order.getOrderDetails().stream().map(OrderDetail::getId).toList()
        ); 
        
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + detail.getQuantity());
                productRepository.save(product);
            }
        }

        return ResponseEntity.ok("Đã hủy đơn hàng thành công.");
    }
}