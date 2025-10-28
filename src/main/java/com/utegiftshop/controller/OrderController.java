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
import com.utegiftshop.security.service.UserDetailsImpl;

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
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice());
            orderDetails.add(detail);
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);

        // === LOGIC MỚI: XỬ LÝ PHƯƠNG THỨC THANH TOÁN ===
        String paymentCode = null;
        if ("SEPAY_QR".equalsIgnoreCase(request.getPaymentMethod())) {
            // Tạo mã thanh toán ngẫu nhiên và duy nhất
            paymentCode = "UTE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setPaymentCode(paymentCode);
            order.setStatus("PENDING_PAYMENT"); // Trạng thái mới: Chờ thanh toán
        }
        // ===============================================

        Order savedOrder = orderRepository.save(order);
        
        List<OrderDetail> savedDetails = new ArrayList<>();
        for(OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            savedDetails.add(orderDetailRepository.save(detail));
        }
        savedOrder.setOrderDetails(savedDetails);

        cartItemRepository.deleteByUserId(userId);

        // Trả về thông tin cần thiết cho frontend
        return ResponseEntity.ok(Map.of(
            "orderId", savedOrder.getId(),
            "paymentMethod", savedOrder.getPaymentMethod(),
            "paymentCode", paymentCode, // Sẽ là null nếu là COD
            "totalAmount", totalAmount
        ));
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