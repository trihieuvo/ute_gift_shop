package com.utegiftshop.controller;

import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderApiController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminOrderApiController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // API 1: Lấy tất cả đơn hàng
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        // Có thể cần dùng DTO nếu Entity Order quá phức tạp
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }
    
    // API 2: Lấy danh sách Shipper
    @GetMapping("/shippers")
    public ResponseEntity<List<User>> getAllShippers() {
        // Giả sử Role Shipper có ID là 4 (hoặc bạn có thể dùng findByRoleName("Shipper"))
        // Tùy thuộc vào cách bạn quản lý Role. Nếu không dùng số, hãy dùng tên.
        // Tạm thời dùng findByRoleName (bạn cần thêm hàm này vào UserRepository)
        List<User> shippers = userRepository.findByRoleName("Shipper"); 
        return ResponseEntity.ok(shippers);
    }

    // API 3: Phân công Shipper cho đơn hàng
    @PostMapping("/{orderId}/assign-shipper/{shipperId}")
    public ResponseEntity<Void> assignShipper(
            @PathVariable Long orderId, 
            @PathVariable Long shipperId) 
    {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        Optional<User> shipperOpt = userRepository.findById(shipperId);

        if (orderOpt.isEmpty() || shipperOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        User shipper = shipperOpt.get();
        
        // Kiểm tra xem người được gán có phải là Shipper không (tùy chọn)
        if (!shipper.getRole().getName().equals("Shipper")) {
            return ResponseEntity.badRequest().build(); // Không phải Shipper
        }

        // Cập nhật Shipper ID
        order.setShipper(shipper); 
        // Admin gán -> Đơn hàng chuyển sang trạng thái "Đang giao" (DELIVERING) nếu đang là 'CONFIRMED'
        if (order.getStatus().equals("CONFIRMED") || order.getStatus().equals("NEW")) {
             order.setStatus("DELIVERING"); 
        }
        
        orderRepository.save(order);
        return ResponseEntity.ok().build();
    }
    // API 4: Lấy chi tiết đơn hàng theo ID
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long orderId) {
    // findById sẽ tự động fetch Order và OrderDetails (nếu Entity được cấu hình FetchType.LAZY)
    // Bạn cần đảm bảo cấu hình JPA cho OrderDetails có thể tải khi cần (dùng @Transactional nếu cần)
    Optional<Order> orderOpt = orderRepository.findById(orderId);

    if (orderOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    // Nếu bạn dùng FetchType.LAZY cho OrderDetails, hãy đảm bảo bạn force load nó ở đây
    // Ví dụ: orderOpt.get().getOrderDetails().size();
    
    return ResponseEntity.ok(orderOpt.get());
    }   
}