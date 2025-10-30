package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // BỔ SUNG

import org.springframework.beans.factory.annotation.Autowired; // BỔ SUNG
import org.springframework.data.domain.Page; // BỔ SUNG
import org.springframework.data.domain.PageRequest; // BỔ SUNG
import org.springframework.data.domain.Pageable; // BỔ SUNG
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // BỔ SUNG
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // BỔ SUNG
import org.springframework.web.bind.annotation.PutMapping; // BỔ SUNG
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.UpdateOrderStatusRequest;
import com.utegiftshop.dto.response.ShipperOrderDetailDto;
import com.utegiftshop.dto.response.ShipperOrderDto;
import com.utegiftshop.dto.response.ShipperStatsDto;
import com.utegiftshop.entity.Order;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.security.service.UserDetailsImpl; 

@RestController
@RequestMapping("/api/shipper")
@PreAuthorize("hasAuthority('Shipper')")
public class ShipperApiController {

    @Autowired
    private OrderRepository orderRepository;

    // === THAY ĐỔI: CÁC TRẠNG THÁI NGHIỆP VỤ MỚI ===
    
    // 1. Đơn hàng đang xử lý (Hiển thị ở trang "Đơn hàng cần giao")
    // BAO GỒM CẢ ĐƠN ĐANG GIAO
    private static final List<String> ACTIVE_STATUSES = List.of("READY_FOR_SHIPMENT", "DELIVERING");
    
    // 2. Đơn hàng đã hoàn tất (Hiển thị ở trang "Lịch sử")
    // THÊM TRẠNG THÁI MỚI 'RETURN_PENDING' và 'RETURNED'
    private static final List<String> COMPLETED_STATUSES = List.of("DELIVERED", "FAILED_DELIVERY", "RETURN_PENDING", "RETURNED"); 
    
    // 3. Các trạng thái Shipper có thể cập nhật
    // THAY 'FAILED_DELIVERY' BẰNG 'RETURN_PENDING'
    private static final Set<String> VALID_SHIPPER_UPDATE_STATUSES = Set.of("DELIVERING", "DELIVERED", "RETURN_PENDING");
    
    // 4. Các trạng thái cuối (Không thể cập nhật nữa)
    private static final Set<String> FINAL_STATUSES = Set.of("DELIVERED", "RETURNED", "CANCELLED");

    // === KẾT THÚC THAY ĐỔI TRẠNG THÁI ===


    private UserDetailsImpl getCurrentShipper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * THAY ĐỔI: Đổi tên API từ "assigned" thành "active"
     * Lấy các đơn hàng ĐANG XỬ LÝ (bao gồm cả 'DELIVERING')
     */
    @GetMapping("/orders/active")
    public ResponseEntity<List<ShipperOrderDto>> getActiveOrders() {
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // THAY ĐỔI: Gọi list trạng thái mới (ACTIVE_STATUSES)
        List<Order> orders = orderRepository.findByShipperIdAndStatusIn(shipperDetails.getId(), ACTIVE_STATUSES);
        if (orders == null || orders.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
        
        List<ShipperOrderDto> orderDtos = orders.stream().map(ShipperOrderDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(orderDtos);
    }
    
    /**
     * THAY ĐỔI: Thêm Phân trang và Lọc theo ngày
     */
    @GetMapping("/orders/completed")
    public ResponseEntity<Page<ShipperOrderDto>> getCompletedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        
        Page<Order> orderPage;
        if (startDate != null && endDate != null) {
            Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(LocalTime.MAX));
            orderPage = orderRepository.findByShipperIdAndStatusInAndOrderDateBetween(
                shipperDetails.getId(), COMPLETED_STATUSES, startTimestamp, endTimestamp, pageable);
        } else {
            orderPage = orderRepository.findByShipperIdAndStatusIn(
                shipperDetails.getId(), COMPLETED_STATUSES, pageable);
        }

        if (orderPage == null || !orderPage.hasContent()) {
            return ResponseEntity.ok(Page.empty());
        }
        
        // Chuyển Page<Order> thành Page<ShipperOrderDto>
        Page<ShipperOrderDto> orderDtoPage = orderPage.map(ShipperOrderDto::new);
        return ResponseEntity.ok(orderDtoPage);
    }

    /**
     * (Giữ nguyên) API lấy chi tiết một đơn hàng 
     */
    @Transactional(readOnly = true) 
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) { 
        UserDetailsImpl shipperDetails = getCurrentShipper();
        if (shipperDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId); 

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getShipper() == null || !order.getShipper().getId().equals(shipperDetails.getId())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem đơn hàng này.");
            }
            ShipperOrderDetailDto dto = new ShipperOrderDetailDto(order); 
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng với ID: " + orderId);
        }
    }

    /**
     * THAY ĐỔI: Xử lý trạng thái mới (RETURN_PENDING) và lưu POD URL
     */
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
        
        // Kiểm tra logic trạng thái
        switch (newStatus) {
            case "DELIVERING":
                if (!"READY_FOR_SHIPMENT".equals(currentStatus)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể bắt đầu giao hàng từ trạng thái 'Sẵn sàng giao'.");
                }
                break;
            case "DELIVERED":
                if (!"DELIVERING".equals(currentStatus)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể xác nhận đã giao hàng từ trạng thái Đang giao hàng.");
                

                break;
            case "RETURN_PENDING": // THAY ĐỔI: Trạng thái mới
                 if (!"DELIVERING".equals(currentStatus)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể báo giao hàng thất bại từ trạng thái Đang giao hàng.");
                 
                 // Yêu cầu lý do thất bại
                 if (!StringUtils.hasText(request.getNote())) {
                    return ResponseEntity.badRequest().body("Vui lòng cung cấp lý do giao hàng thất bại.");
                 }
                 break;
            default:
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Trạng thái cập nhật không xác định.");
        }

        order.setStatus(newStatus);
        if (StringUtils.hasText(request.getNote())) order.setDeliveryNote(request.getNote());
        
        orderRepository.save(order);
        return ResponseEntity.ok("Cập nhật trạng thái đơn hàng #" + orderId + " thành công thành: " + newStatus);
    }

     /**
      * THAY ĐỔI: Bổ sung tính toán tiền COD
      */
     @GetMapping("/stats")
     public ResponseEntity<?> getShipperStats() {
         UserDetailsImpl shipperDetails = getCurrentShipper();
         if (shipperDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         try {
             // Đơn đang xử lý (ACTIVE_STATUSES)
             long assignedCount = orderRepository.countByShipperIdAndStatusIn(shipperDetails.getId(), ACTIVE_STATUSES);
             
             // Đơn đã giao (Chỉ DELIVERED)
             long deliveredCount = orderRepository.countByShipperIdAndStatus(shipperDetails.getId(), "DELIVERED");
             
             // Đơn thất bại (Bao gồm cả đang chờ trả và đã trả)
             long failedCount = orderRepository.countByShipperIdAndStatusIn(shipperDetails.getId(), List.of("RETURN_PENDING", "RETURNED", "FAILED_DELIVERY"));
             
             // Tiền COD đang giữ
             BigDecimal totalCod = orderRepository.sumTotalCodByShipperAndStatusDeliveredAndNotReconciled(shipperDetails.getId());
             if (totalCod == null) {
                 totalCod = BigDecimal.ZERO;
             }
             
             ShipperStatsDto stats = new ShipperStatsDto(assignedCount, deliveredCount, failedCount, totalCod);
             return ResponseEntity.ok(stats);
         } catch (Exception e) {
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy dữ liệu thống kê: " + e.getMessage());
         }
     }
}