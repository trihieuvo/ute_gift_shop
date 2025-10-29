package com.utegiftshop.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderApiController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminOrderApiController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // API 1: Lấy tất cả đơn hàng (ĐÃ THÊM BỘ LỌC)
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) String status
    ) {
        // Xây dựng Specification (tiêu chí lọc)
        Specification<Order> spec = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            // Lọc theo Trạng thái
            if (status != null && !status.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            
            // Lọc theo Ngày Bắt đầu
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    // Chuyển đổi ngày yyyy-MM-dd thành Timestamp lúc 00:00:00
                    Timestamp startTimestamp = Timestamp.valueOf(startDate + " 00:00:00");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"), startTimestamp));
                } catch (IllegalArgumentException ignored) {} 
            }
            
            // Lọc theo Ngày Kết thúc
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    // Chuyển đổi ngày yyyy-MM-dd thành Timestamp lúc 23:59:59
                    Timestamp endTimestamp = Timestamp.valueOf(endDate + " 23:59:59");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"), endTimestamp));
                } catch (IllegalArgumentException ignored) {} 
            }

            // Sắp xếp theo ngày mới nhất
            query.orderBy(criteriaBuilder.desc(root.get("orderDate")));

            return predicate;
        };

        // Sử dụng findAll(Specification) để lọc
        List<Order> orders = orderRepository.findAll(spec);
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
        // KHÔNG tự động chuyển trạng thái nữa. Việc này sẽ do Shipper thực hiện.
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