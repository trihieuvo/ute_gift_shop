package com.utegiftshop.controller;

import com.utegiftshop.dto.request.UpdateOrderStatusRequest;
import com.utegiftshop.dto.response.ShipperStatsDto; 
import com.utegiftshop.dto.response.ShipperOrderDto;
import com.utegiftshop.dto.response.ShipperOrderDetailDto; // <-- BỔ SUNG IMPORT
import com.utegiftshop.entity.Order;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils; 
import org.springframework.transaction.annotation.Transactional; // <-- BỔ SUNG IMPORT
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp; 
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set; 
import java.util.stream.Collectors; 

@RestController
@RequestMapping("/api/shipper")
@PreAuthorize("hasAuthority('Shipper')")
public class ShipperApiController {

    @Autowired
    private OrderRepository orderRepository;

    // (Giữ nguyên các hằng số)
    private static final List<String> ASSIGNED_STATUSES = List.of("CONFIRMED", "PREPARING");
    private static final List<String> ACTIVE_STATUSES_FOR_DASHBOARD = List.of("CONFIRMED", "PREPARING", "DELIVERING");
    private static final List<String> COMPLETED_STATUSES = List.of("DELIVERED", "FAILED_DELIVERY"); 
    private static final Set<String> VALID_SHIPPER_UPDATE_STATUSES = Set.of("DELIVERING", "DELIVERED", "FAILED_DELIVERY");
    private static final Set<String> FINAL_STATUSES = Set.of("DELIVERED", "FAILED_DELIVERY", "CANCELLED", "RETURNED");

    private UserDetailsImpl getCurrentShipper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        return null;
    }

    // (Giữ nguyên API /orders/assigned)
    @GetMapping("/orders/assigned")
    public ResponseEntity<List<ShipperOrderDto>> getAssignedOrders() {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Order> orders = orderRepository.findByShipperIdAndStatusIn(shipperDetails.getId(), ASSIGNED_STATUSES);
        if (orders == null || orders.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
        List<ShipperOrderDto> orderDtos = orders.stream().map(ShipperOrderDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(orderDtos);
    }
    
    // (Giữ nguyên API /orders/completed)
    @GetMapping("/orders/completed")
    public ResponseEntity<List<ShipperOrderDto>> getCompletedOrders() {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Order> orders = orderRepository.findByShipperIdAndStatusIn(shipperDetails.getId(), COMPLETED_STATUSES);
        if (orders == null || orders.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
        List<ShipperOrderDto> orderDtos = orders.stream().map(ShipperOrderDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(orderDtos);
    }

    /**
     * API lấy chi tiết một đơn hàng (SỬ DỤNG DTO)
     */
    // === BỔ SUNG LẠI @Transactional VÀ THAY ĐỔI KIỂU TRẢ VỀ ===
    @Transactional(readOnly = true) 
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) { 
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Vẫn dùng findById để lấy Order Entity (vì cần load OrderDetails EAGER)
        Optional<Order> orderOpt = orderRepository.findById(orderId); 

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Kiểm tra quyền sở hữu
            if (order.getShipper() == null || !order.getShipper().getId().equals(shipperDetails.getId())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem đơn hàng này.");
            }
            // Chuyển đổi Entity sang DTO trước khi trả về
            ShipperOrderDetailDto dto = new ShipperOrderDetailDto(order); 
            return ResponseEntity.ok(dto); // Trả về DTO
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng với ID: " + orderId);
        }
    }
    // === KẾT THÚC THAY ĐỔI ===

    // (Giữ nguyên API updateOrderStatus)
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest request) {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Yêu cầu xác thực.");

        String newStatus = request.getNewStatus();
        if (newStatus == null || newStatus.isBlank() || !VALID_SHIPPER_UPDATE_STATUSES.contains(newStatus)) {
            return ResponseEntity.badRequest().body("Trạng thái cập nhật không hợp lệ. Chỉ chấp nhận: " + VALID_SHIPPER_UPDATE_STATUSES);
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng với ID: " + orderId);

        Order order = orderOpt.get();
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền cập nhật đơn hàng này.");
        }

        String currentStatus = order.getStatus();
        if (FINAL_STATUSES.contains(currentStatus)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể cập nhật trạng thái cho đơn hàng đã kết thúc (Hiện tại: " + currentStatus + ").");
        }
        
        if ("FAILED_DELIVERY".equals(newStatus) && !StringUtils.hasText(request.getNote())) {
            return ResponseEntity.badRequest().body("Vui lòng cung cấp lý do giao hàng thất bại.");
        }

        switch (newStatus) {
            case "DELIVERING":
                if (!ASSIGNED_STATUSES.contains(currentStatus)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể bắt đầu giao hàng từ trạng thái " + ASSIGNED_STATUSES);
                break;
            case "DELIVERED":
                if (!"DELIVERING".equals(currentStatus)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể xác nhận đã giao hàng từ trạng thái Đang giao hàng.");
                break;
            case "FAILED_DELIVERY":
                 if (!"DELIVERING".equals(currentStatus)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể báo giao hàng thất bại từ trạng thái Đang giao hàng.");
                 break;
            default:
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Trạng thái cập nhật không xác định.");
        }

        order.setStatus(newStatus);
        if (StringUtils.hasText(request.getNote())) order.setDeliveryNote(request.getNote());
        orderRepository.save(order);
        return ResponseEntity.ok("Cập nhật trạng thái đơn hàng #" + orderId + " thành công thành: " + newStatus);
    }

     // (Giữ nguyên API /stats)
     @GetMapping("/stats")
     public ResponseEntity<?> getShipperStats() {
         UserDetailsImpl shipperDetails = getCurrentShipper();
         if (shipperDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         try {
             long assignedCount = orderRepository.countByShipperIdAndStatusIn(shipperDetails.getId(), ACTIVE_STATUSES_FOR_DASHBOARD);
             long deliveredCount = orderRepository.countByShipperIdAndStatus(shipperDetails.getId(), "DELIVERED");
             long failedCount = orderRepository.countByShipperIdAndStatus(shipperDetails.getId(), "FAILED_DELIVERY");
             ShipperStatsDto stats = new ShipperStatsDto(assignedCount, deliveredCount, failedCount);
             return ResponseEntity.ok(stats);
         } catch (Exception e) {
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy dữ liệu thống kê.");
         }
     }
}