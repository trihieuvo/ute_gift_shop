package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap; // <-- Đảm bảo import HashMap
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
import com.utegiftshop.entity.ShippingMethod;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.AddressRepository;
import com.utegiftshop.repository.CartItemRepository;
import com.utegiftshop.repository.OrderDetailRepository;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.ShippingMethodRepository;
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
    @Autowired private ShippingMethodRepository shippingMethodRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        throw new IllegalStateException("Không thể xác định người dùng hiện tại.");
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
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ hoặc không tồn tại. ID: " + request.getAddressId()));

        ShippingMethod shippingMethod = shippingMethodRepository.findById(request.getShippingMethodId())
                .orElseThrow(() -> new RuntimeException("Đơn vị vận chuyển không hợp lệ."));
        BigDecimal shippingFee = shippingMethod.getFee();

        if (!address.getUser().getId().equals(userId)) {
             throw new SecurityException("Địa chỉ không thuộc về người dùng này.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(address.getFullAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingMethod(shippingMethod);
        order.setShippingFee(shippingFee);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();
        Map<Long, Product> productCache = new HashMap<>();

        for (CartItem item : cartItems) {
            Long productId = item.getProduct().getId();
            Product product = productCache.computeIfAbsent(productId, id ->
                 productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id + " trong giỏ hàng."))
            );

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho.");
            }
            
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice());

          
            if (product.getShop() != null && product.getShop().getCommissionRate() != null) {
                detail.setCommissionRate(product.getShop().getCommissionRate());
            } else {
                detail.setCommissionRate(BigDecimal.ZERO); // Gán mặc định là 0
            }
            

            orderDetails.add(detail);
            subtotal = subtotal.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
        }
        BigDecimal finalTotalAmount = subtotal.add(shippingFee);
        order.setTotalAmount(finalTotalAmount);
        order.setOrderDetails(orderDetails);

        String paymentCode = null;
        if ("SEPAY_QR".equalsIgnoreCase(request.getPaymentMethod())) {
            paymentCode = "UTE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setPaymentCode(paymentCode);
            order.setStatus("PENDING_PAYMENT"); // Trạng thái chờ thanh toán QR
        } else if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setStatus("NEW"); // Trạng thái mới cho đơn COD
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không được hỗ trợ."));
        }

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("orderId", savedOrder.getId());
        responseBody.put("paymentMethod", savedOrder.getPaymentMethod());
        responseBody.put("paymentCode", paymentCode);
        responseBody.put("totalAmount", finalTotalAmount);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        Long userId = getCurrentUser().getId();
        List<Order> orders = orderRepository.findByUserId(userId);
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Optional<Order> orderOpt = orderRepository.findByIdAndUserId(orderId, userId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.getOrderDetails().size();
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này."));
        }
    }

    @PutMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền thao tác với đơn hàng này."));

        if (!"NEW".equals(order.getStatus()) && !"PENDING_PAYMENT".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ có thể hủy đơn hàng khi ở trạng thái 'Mới' hoặc 'Chờ thanh toán'. Trạng thái hiện tại: " + order.getStatus()));
        }

        order.setStatus("CANCELLED");

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    Product currentProduct = productRepository.findById(product.getId())
                            .orElse(null);
                    if (currentProduct != null) {
                        currentProduct.setStockQuantity(currentProduct.getStockQuantity() + detail.getQuantity());
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công và cập nhật lại tồn kho."));
    }

    // === API MỚI: ĐỔI PHƯƠNG THỨC THANH TOÁN ===
    @PutMapping("/{orderId}/change-payment")
    @Transactional
    public ResponseEntity<?> changePaymentMethod(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> payload) {
        Long userId = getCurrentUser().getId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền thao tác."));

        // Chỉ cho phép đổi khi trạng thái là NEW hoặc PENDING_PAYMENT
        if (!"NEW".equals(order.getStatus()) && !"PENDING_PAYMENT".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ có thể thay đổi phương thức thanh toán cho đơn hàng ở trạng thái 'Mới' hoặc 'Chờ thanh toán'."));
        }

        String newPaymentMethod = payload.get("newPaymentMethod");
        String currentPaymentMethod = order.getPaymentMethod();
        String currentStatus = order.getStatus(); // Lấy trạng thái hiện tại

        if (newPaymentMethod == null || newPaymentMethod.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán mới không được để trống."));
        }

        // === SỬA LỖI SYNTAX Ở ĐÂY ===
        // Kiểm tra xem có thực sự thay đổi không
        if (newPaymentMethod.equalsIgnoreCase(currentPaymentMethod) ||
            ("SEPAY_QR".equalsIgnoreCase(newPaymentMethod) && "PENDING_PAYMENT".equals(currentStatus)) || // Nếu đang chờ QR mà vẫn chọn QR
            ("COD".equalsIgnoreCase(newPaymentMethod) && "NEW".equals(currentStatus)) ) { // Nếu đang NEW (COD) mà vẫn chọn COD
            // Tạo Map đúng cách
            Map<String, Object> noChangeResponse = new HashMap<>();
            noChangeResponse.put("message", "Phương thức thanh toán không thay đổi.");
            noChangeResponse.put("status", order.getStatus());
            noChangeResponse.put("paymentCode", order.getPaymentCode());
            return ResponseEntity.ok(noChangeResponse); // <-- Thêm dấu ;
        }
        // === KẾT THÚC SỬA LỖI SYNTAX ===

        Map<String, Object> responseBody = new HashMap<>(); // Dùng HashMap

        if ("SEPAY_QR".equalsIgnoreCase(newPaymentMethod)) {
            // Đổi từ COD (trạng thái NEW) sang QR
             if (!"NEW".equals(currentStatus)) {
                  return ResponseEntity.badRequest().body(Map.of("message", "Không thể đổi sang QR từ trạng thái hiện tại."));
             }
            String paymentCode = "UTE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setPaymentMethod("SEPAY_QR");
            order.setPaymentCode(paymentCode);
            order.setStatus("PENDING_PAYMENT"); // Chuyển sang chờ thanh toán QR
            orderRepository.save(order);
            responseBody.put("message", "Đã đổi sang thanh toán QR. Vui lòng thanh toán.");
            responseBody.put("status", order.getStatus());
            responseBody.put("paymentCode", paymentCode);
            responseBody.put("totalAmount", order.getTotalAmount()); // Thêm tổng tiền để chuyển trang payment
            responseBody.put("paymentMethod", order.getPaymentMethod()); // Trả về PTTT mới

        } else if ("COD".equalsIgnoreCase(newPaymentMethod)) {
            // Đổi từ QR (trạng thái PENDING_PAYMENT) sang COD
            if (!"PENDING_PAYMENT".equals(currentStatus)) {
                  return ResponseEntity.badRequest().body(Map.of("message", "Chỉ có thể đổi về COD từ trạng thái 'Chờ thanh toán QR'."));
            }
            order.setPaymentMethod("COD");
            order.setPaymentCode(null); // Xóa mã QR cũ
            order.setStatus("NEW"); // Trở về trạng thái mới
            orderRepository.save(order);
            responseBody.put("message", "Đã đổi sang thanh toán COD.");
            responseBody.put("status", order.getStatus());
            responseBody.put("paymentCode", null);
            responseBody.put("paymentMethod", order.getPaymentMethod()); // Trả về PTTT mới
            // === SỬA LỖI SYNTAX Ở ĐÂY (Xóa dấu ); thừa) ===
            // Không có dấu ); thừa ở đây
            // === KẾT THÚC SỬA LỖI SYNTAX ===

        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không hợp lệ."));
        }

        return ResponseEntity.ok(responseBody);
    }
    // === KẾT THÚC API MỚI ===
}