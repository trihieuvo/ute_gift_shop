package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.CheckoutRequest;
import com.utegiftshop.entity.Address;
import com.utegiftshop.entity.CartItem;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.OrderDetail;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.AddressRepository;
import com.utegiftshop.repository.CartItemRepository;
import com.utegiftshop.repository.OrderDetailRepository;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

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
            return ResponseEntity.badRequest().body(Map.of("message", "Giỏ hàng của bạn đang trống."));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ."));

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(address.getFullAddress());
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + item.getProduct().getId()));
            
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho.");
            }
            
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice());
            orderDetails.add(detail);
            
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            // Không cần save product ở đây, @Transactional sẽ quản lý
        }

        order.setTotalAmount(totalAmount);
        order.setOrderDetails(orderDetails); // Gán danh sách chi tiết VÀO đơn hàng

        String paymentCode = null;
        if ("SEPAY_QR".equalsIgnoreCase(request.getPaymentMethod())) {
            paymentCode = "UTE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setPaymentCode(paymentCode);
            order.setStatus("PENDING_PAYMENT");
        } else {
            order.setStatus("NEW");
        }

        Order savedOrder = orderRepository.save(order); // Chỉ cần lưu Order, OrderDetail sẽ được lưu theo
        
        cartItemRepository.deleteByUserId(userId);

        return ResponseEntity.ok(Map.of(
            "orderId", savedOrder.getId(),
            "paymentMethod", savedOrder.getPaymentMethod(),
            "paymentCode", paymentCode,
            "totalAmount", totalAmount
        ));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Optional<Order> orderOpt = orderRepository.findByIdAndUserId(orderId, userId);
        return orderOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Không tìm thấy đơn hàng hoặc bạn không có quyền xem."));
    }
    
    @PutMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền thao tác."));

        if (!"NEW".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng ở trạng thái 'Mới' (NEW).");
        }

        order.setStatus("CANCELLED");

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + detail.getQuantity());
            }
        }
        
        return ResponseEntity.ok("Đã hủy đơn hàng thành công.");
    }
}