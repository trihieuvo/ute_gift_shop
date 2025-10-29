package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap; // <-- THÊM IMPORT NÀY
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
import com.utegiftshop.repository.OrderDetailRepository; // Đảm bảo đã import
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository; // Đảm bảo đã Autowired
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra null và kiểu dữ liệu trước khi ép kiểu
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        // Ném exception hoặc trả về null tùy logic xử lý lỗi của bạn
        throw new IllegalStateException("Không thể xác định người dùng hiện tại.");
    }

    @PostMapping("/checkout")
    @Transactional // Rất quan trọng để đảm bảo tính nhất quán dữ liệu
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

        // Security check: Đảm bảo địa chỉ thuộc về user
        if (!address.getUser().getId().equals(userId)) {
             throw new SecurityException("Địa chỉ không thuộc về người dùng này.");
        }


        Order order = new Order();
        order.setUser(user);
        // Lưu toàn bộ thông tin địa chỉ, không chỉ chuỗi string
        // Điều này tốt hơn cho việc truy vấn sau này, nhưng yêu cầu thay đổi entity Order
        // Tạm thời giữ nguyên theo code gốc của bạn:
        order.setShippingAddress(address.getFullAddress()); // Hoặc kết hợp các trường nếu cần
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        // Sử dụng Set để tránh load product nhiều lần nếu mua cùng SP từ nhiều nguồn
        Map<Long, Product> productCache = new HashMap<>();

        for (CartItem item : cartItems) {
            Long productId = item.getProduct().getId();
            Product product = productCache.computeIfAbsent(productId, id ->
                 productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id + " trong giỏ hàng."))
            );


            // Kiểm tra tồn kho trước khi tạo OrderDetail
            if (product.getStockQuantity() < item.getQuantity()) {
                // Ném lỗi rõ ràng hơn
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' (ID: " + productId +") không đủ số lượng trong kho. Yêu cầu: " + item.getQuantity() + ", Còn lại: " + product.getStockQuantity());
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order); // Liên kết chi tiết với đơn hàng
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice()); // Lưu giá tại thời điểm mua
            orderDetails.add(detail);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));

            // Trừ tồn kho
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            // Không cần gọi productRepository.save(product) ở đây vì @Transactional
        }

        order.setTotalAmount(totalAmount);
        order.setOrderDetails(orderDetails); // Quan trọng: Gán danh sách chi tiết VÀO đơn hàng

        String paymentCode = null;
        if ("SEPAY_QR".equalsIgnoreCase(request.getPaymentMethod())) {
            // Tạo mã thanh toán duy nhất
            paymentCode = "UTE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setPaymentCode(paymentCode);
            order.setStatus("PENDING_PAYMENT"); // Trạng thái chờ thanh toán
            order.setPaymentStatus("PENDING");
        } else if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setStatus("NEW"); // Trạng thái mới cho đơn COD
            order.setPaymentStatus("PENDING"); // COD cũng là chờ thanh toán (khi nhận hàng)
        } else {
            // Xử lý các phương thức thanh toán khác nếu có
            return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không được hỗ trợ."));
        }

        // Lưu đơn hàng (và các chi tiết đơn hàng nhờ CascadeType.ALL)
        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đặt hàng thành công
        cartItemRepository.deleteByUserId(userId);

        // === SỬA LỖI Ở ĐÂY: Dùng HashMap thay vì Map.of ===
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("orderId", savedOrder.getId());
        responseBody.put("paymentMethod", savedOrder.getPaymentMethod());
        responseBody.put("paymentCode", paymentCode); // HashMap cho phép giá trị null
        responseBody.put("totalAmount", totalAmount); // Có thể dùng savedOrder.getTotalAmount()
        return ResponseEntity.ok(responseBody);
        // === KẾT THÚC SỬA LỖI ===
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        Long userId = getCurrentUser().getId();
        // Sắp xếp theo ngày đặt mới nhất
        List<Order> orders = orderRepository.findByUserId(userId);
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Transactional(readOnly = true) // Cần transactional để load lazy OrderDetails
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        Long userId = getCurrentUser().getId();
        Optional<Order> orderOpt = orderRepository.findByIdAndUserId(orderId, userId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Đảm bảo OrderDetails được load (không cần thiết nếu EAGER)
            order.getOrderDetails().size(); // Trigger loading if lazy
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này."));
        }
    }

    @PutMapping("/{orderId}/cancel")
    @Transactional // Cần Transactional để cập nhật tồn kho và trạng thái đơn hàng
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) { // Đổi void thành ResponseEntity<?>
        Long userId = getCurrentUser().getId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền thao tác với đơn hàng này."));

        // Chỉ cho phép hủy khi trạng thái là NEW hoặc PENDING_PAYMENT
        if (!"NEW".equals(order.getStatus()) && !"PENDING_PAYMENT".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ có thể hủy đơn hàng khi ở trạng thái 'Mới' hoặc 'Chờ thanh toán'. Trạng thái hiện tại: " + order.getStatus()));
        }

        order.setStatus("CANCELLED"); // Cập nhật trạng thái

        // Hoàn trả lại số lượng tồn kho cho các sản phẩm trong đơn hàng bị hủy
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    // Lấy lại thông tin product mới nhất từ DB để tránh xung đột
                    Product currentProduct = productRepository.findById(product.getId())
                            .orElse(null); // Hoặc xử lý lỗi nếu sản phẩm bị xóa
                    if (currentProduct != null) {
                        currentProduct.setStockQuantity(currentProduct.getStockQuantity() + detail.getQuantity());
                        // productRepository.save(currentProduct) // Không cần save riêng lẻ nhờ @Transactional
                    }
                }
            }
        }

        // orderRepository.save(order); // Không cần save riêng lẻ nhờ @Transactional
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công và cập nhật lại tồn kho.")); // Trả về thông báo thành công
    }
}