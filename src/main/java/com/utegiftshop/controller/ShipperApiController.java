package com.utegiftshop.controller;

import com.utegiftshop.dto.request.UpdateOrderStatusRequest;
import com.utegiftshop.dto.response.ShipperStatsDto; // Import DTO mới
import com.utegiftshop.entity.Order;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp; // Import Timestamp để cập nhật thời gian
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set để kiểm tra trạng thái hợp lệ

@RestController
@RequestMapping("/api/shipper")
@PreAuthorize("hasAuthority('Shipper')")
public class ShipperApiController {

    @Autowired
    private OrderRepository orderRepository;

    // Các trạng thái mà Shipper cần xử lý (được Admin phân công)
    private static final List<String> ASSIGNED_STATUSES = List.of("CONFIRMED", "PREPARING");
    // Các trạng thái hợp lệ mà Shipper có thể cập nhật
    private static final Set<String> VALID_SHIPPER_UPDATE_STATUSES = Set.of("DELIVERING", "DELIVERED", "FAILED_DELIVERY");
    // Các trạng thái cuối cùng (không thể cập nhật nữa)
    private static final Set<String> FINAL_STATUSES = Set.of("DELIVERED", "FAILED_DELIVERY", "CANCELLED", "RETURNED"); // Thêm RETURNED nếu có

    private UserDetailsImpl getCurrentShipper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        // Trong môi trường thực tế, nên ném một Exception ở đây thay vì trả về null
        // throw new InsufficientAuthenticationException("Shipper not authenticated");
        return null;
    }

    @GetMapping("/orders/assigned")
    public ResponseEntity<List<Order>> getAssignedOrders() {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Order> orders = orderRepository.findByShipperIdAndStatusIn(shipperDetails.getId(), ASSIGNED_STATUSES);
        // Trả về danh sách rỗng nếu không có đơn hàng, thay vì null
        return ResponseEntity.ok(orders == null ? Collections.emptyList() : orders);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // **Kiểm tra quyền sở hữu:** Đơn hàng phải được gán cho shipper này
            if (order.getShipper() == null || !order.getShipper().getId().equals(shipperDetails.getId())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem đơn hàng này.");
            }
             // TODO: Cân nhắc trả về DTO thay vì Entity để kiểm soát dữ liệu gửi đi
            // Ví dụ: chỉ trả về thông tin cần thiết cho shipper
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng với ID: " + orderId);
        }
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest request) {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Yêu cầu xác thực.");
        }

        String newStatus = request.getNewStatus();
        // **Kiểm tra đầu vào:** Trạng thái mới có hợp lệ không?
        if (newStatus == null || newStatus.isBlank() || !VALID_SHIPPER_UPDATE_STATUSES.contains(newStatus)) {
            return ResponseEntity.badRequest().body("Trạng thái cập nhật không hợp lệ. Chỉ chấp nhận: " + VALID_SHIPPER_UPDATE_STATUSES);
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng với ID: " + orderId);
        }

        Order order = orderOpt.get();

        // **Kiểm tra quyền sở hữu:** Đơn hàng phải được gán cho shipper này
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền cập nhật đơn hàng này.");
        }

        String currentStatus = order.getStatus();

        // **Kiểm tra logic chuyển trạng thái:**
        if (FINAL_STATUSES.contains(currentStatus)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể cập nhật trạng thái cho đơn hàng đã kết thúc (Hiện tại: " + currentStatus + ").");
        }

        // Logic cụ thể hơn (ví dụ)
        switch (newStatus) {
            case "DELIVERING":
                // Chỉ cho phép chuyển sang Đang giao từ các trạng thái trước đó (CONFIRMED, PREPARING)
                if (!ASSIGNED_STATUSES.contains(currentStatus)) {
                     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể bắt đầu giao hàng từ trạng thái " + ASSIGNED_STATUSES);
                }
                break;
            case "DELIVERED":
                 // Chỉ cho phép chuyển sang Đã giao từ Đang giao
                if (!"DELIVERING".equals(currentStatus)) {
                     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể xác nhận đã giao hàng từ trạng thái Đang giao hàng.");
                }
                // TODO: Có thể thêm logic cập nhật thời gian giao hàng thành công
                // order.setDeliveredAt(new Timestamp(System.currentTimeMillis()));
                break;
            case "FAILED_DELIVERY":
                // Cho phép chuyển sang Giao thất bại từ Đang giao
                 if (!"DELIVERING".equals(currentStatus)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể báo giao hàng thất bại từ trạng thái Đang giao hàng.");
                 }
                // TODO: Có thể yêu cầu shipper thêm lý do thất bại (cần thêm trường 'note' vào request và order)
                // if (request.getNote() == null || request.getNote().isBlank()) {
                //    return ResponseEntity.badRequest().body("Vui lòng cung cấp lý do giao hàng thất bại.");
                // }
                // order.setDeliveryNote(request.getNote());
                break;
            default:
                // Trường hợp này không nên xảy ra do đã kiểm tra VALID_SHIPPER_UPDATE_STATUSES
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Trạng thái cập nhật không xác định.");
        }

        order.setStatus(newStatus);
        // Cập nhật thời gian update (nếu Order entity có trường updatedAt)
        // order.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        orderRepository.save(order);
        return ResponseEntity.ok("Cập nhật trạng thái đơn hàng #" + orderId + " thành công thành: " + newStatus);

    }

     @GetMapping("/stats")
     public ResponseEntity<?> getShipperStats() {
         UserDetailsImpl shipperDetails = getCurrentShipper();
         if (shipperDetails == null) {
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }

         try {
             // Đếm đơn đang chờ/cần giao (CONFIRMED, PREPARING, DELIVERING)
             List<String> activeStatuses = List.of("CONFIRMED", "PREPARING", "DELIVERING");
             long assignedCount = orderRepository.countByShipperIdAndStatusIn(shipperDetails.getId(), activeStatuses);

             // Đếm đơn đã giao thành công
             long deliveredCount = orderRepository.countByShipperIdAndStatus(shipperDetails.getId(), "DELIVERED");

             // Đếm đơn giao thất bại
             long failedCount = orderRepository.countByShipperIdAndStatus(shipperDetails.getId(), "FAILED_DELIVERY");

             ShipperStatsDto stats = new ShipperStatsDto(assignedCount, deliveredCount, failedCount);
             return ResponseEntity.ok(stats);

         } catch (Exception e) {
              // Ghi log lỗi ở đây
              // log.error("Error fetching shipper stats for shipperId {}: {}", shipperDetails.getId(), e.getMessage());
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy dữ liệu thống kê.");
         }
     }
}