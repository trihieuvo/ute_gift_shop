package com.utegiftshop.controller;

import com.utegiftshop.dto.response.VendorOrderDetailDto;
import com.utegiftshop.entity.*;
import com.utegiftshop.repository.*;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/vendor")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorOrderController {

    private static final Logger logger = LoggerFactory.getLogger(VendorOrderController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;

    /**
     * Helper method để lấy Shop của vendor đang đăng nhập
     */
    private Shop getAuthenticatedShop(UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Xác thực không thành công.");
        }
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.error("Shop not found for user ID: {}", userId);
                    return new RuntimeException("Cửa hàng của bạn chưa được thiết lập.");
                });
    }

    /**
     * GET /api/vendor/orders - Lấy danh sách đơn hàng của vendor
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getVendorOrders(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("📦 Fetching orders for Shop ID: {}", shopId);

            List<Product> shopProducts = productRepository.findByShopId(shopId);
            if (shopProducts.isEmpty()) {
                logger.info("Shop {} has no products", shopId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            Set<Long> shopProductIds = new HashSet<>();
            for (Product p : shopProducts) {
                shopProductIds.add(p.getId());
            }

            List<Order> allOrders = orderRepository.findAll();
            List<Map<String, Object>> vendorOrders = new ArrayList<>();

            for (Order order : allOrders) {
                List<OrderDetail> vendorDetails = new ArrayList<>();
                BigDecimal vendorTotal = BigDecimal.ZERO;

                for (OrderDetail detail : order.getOrderDetails()) {
                    if (detail.getProduct() != null
                            && shopProductIds.contains(detail.getProduct().getId())) {
                        vendorDetails.add(detail);
                        BigDecimal itemTotal = detail.getPrice()
                                .multiply(new BigDecimal(detail.getQuantity()));
                        vendorTotal = vendorTotal.add(itemTotal);
                    }
                }

                if (vendorDetails.isEmpty()) {
                    continue;
                }

                if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
                    if (!order.getStatus().equalsIgnoreCase(status)) {
                        continue;
                    }
                }

                Map<String, Object> orderDto = new HashMap<>();
                orderDto.put("orderId", order.getId());
                orderDto.put("orderDate", order.getOrderDate());
                orderDto.put("status", order.getStatus());
                orderDto.put("paymentMethod", order.getPaymentMethod());
                orderDto.put("shippingAddress", order.getShippingAddress());
                orderDto.put("deliveryNote", order.getDeliveryNote());

                if (order.getUser() != null) {
                    orderDto.put("customerName", order.getUser().getFullName());
                    orderDto.put("customerEmail", order.getUser().getEmail());
                    orderDto.put("customerPhone", order.getUser().getPhoneNumber());
                }

                orderDto.put("vendorTotalAmount", vendorTotal);
                orderDto.put("vendorItemCount", vendorDetails.size());

                vendorOrders.add(orderDto);
            }

            vendorOrders.sort((a, b) -> {
                Long idA = (Long) a.get("orderId");
                Long idB = (Long) b.get("orderId");
                return idB.compareTo(idA);
            });

            logger.info("✅ Found {} orders for Shop ID: {}", vendorOrders.size(), shopId);
            return ResponseEntity.ok(vendorOrders);

        } catch (RuntimeException e) {
            logger.error("❌ Error in getVendorOrders: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            logger.error("❌ Unexpected error in getVendorOrders:", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/vendor/orders/{orderId} - Chi tiết đơn hàng
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getVendorOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("📋 Fetching order detail {} for Shop {}", orderId, shopId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            List<VendorOrderDetailDto> vendorItems = new ArrayList<>();
            BigDecimal vendorTotal = BigDecimal.ZERO;

            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() != null
                        && detail.getProduct().getShop() != null
                        && detail.getProduct().getShop().getId().equals(shopId)) {
                    VendorOrderDetailDto itemDto = new VendorOrderDetailDto(detail);
                    vendorItems.add(itemDto);
                    vendorTotal = vendorTotal.add(itemDto.getTotalItemPrice());
                }
            }

            if (vendorItems.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Đơn hàng này không chứa sản phẩm của bạn");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("orderDate", order.getOrderDate());
            response.put("status", order.getStatus());
            response.put("paymentMethod", order.getPaymentMethod());
            response.put("shippingAddress", order.getShippingAddress());
            response.put("deliveryNote", order.getDeliveryNote());

            if (order.getUser() != null) {
                Map<String, String> customer = new HashMap<>();
                customer.put("name", order.getUser().getFullName());
                customer.put("email", order.getUser().getEmail());
                customer.put("phone", order.getUser().getPhoneNumber());
                response.put("customer", customer);
            }

            response.put("vendorTotalAmount", vendorTotal);
            response.put("vendorItems", vendorItems);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error in getVendorOrderDetail: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            HttpStatus status = e.getMessage().contains("không tồn tại")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status).body(error);
        } catch (Exception e) {
            logger.error("❌ Unexpected error in getVendorOrderDetail:", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /api/vendor/orders/{orderId}/status - Cập nhật trạng thái đơn hàng
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();

            String newStatus = payload.get("newStatus");
            if (newStatus == null || newStatus.isBlank()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Trạng thái mới không hợp lệ");
                return ResponseEntity.badRequest().body(error);
            }
            newStatus = newStatus.toUpperCase();

            logger.info("🔄 Updating order {} to status {} for Shop {}", orderId, newStatus, shopId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            boolean hasProduct = false;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() != null
                        && detail.getProduct().getShop() != null
                        && detail.getProduct().getShop().getId().equals(shopId)) {
                    hasProduct = true;
                    break;
                }
            }

            if (!hasProduct) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không có quyền cập nhật đơn hàng này");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Validate status transition
            Set<String> allowedStatuses = new HashSet<>(Arrays.asList(
                    "CONFIRMED", "PREPARING", "READY_FOR_SHIPMENT", "RETURNED"));

            if (!allowedStatuses.contains(newStatus)) {
                throw new RuntimeException("Trạng thái '" + newStatus + "' không hợp lệ");
            }

            String currentStatus = order.getStatus();
            boolean isValid = false;

            switch (newStatus) {
                case "CONFIRMED":
                    isValid = "NEW".equals(currentStatus);
                    break;
                case "PREPARING":
                    isValid = "CONFIRMED".equals(currentStatus);
                    break;
                case "READY_FOR_SHIPMENT":
                    isValid = "PREPARING".equals(currentStatus);
                    break;
                case "RETURNED":
                	isValid = "RETURN_PENDING".equals(currentStatus) || "DELIVERED".equals(currentStatus);
                    break;
            }

            if (!isValid) {
                throw new RuntimeException("Không thể chuyển từ '" + currentStatus + "' sang '" + newStatus + "'");
            }

            order.setStatus(newStatus);
            orderRepository.save(order);

            logger.info("✅ Order {} status updated to {}", orderId, newStatus);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Cập nhật trạng thái thành công");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error updating status: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            HttpStatus status = e.getMessage().contains("không tồn tại")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error);
        } catch (Exception e) {
            logger.error("❌ Unexpected error updating status:", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/vendor/orders/statistics - Thống kê đơn hàng
     */
    @GetMapping("/orders/statistics")
    public ResponseEntity<?> getOrderStatistics(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("📊 Calculating statistics for Shop {}", shopId);

            List<Product> shopProducts = productRepository.findByShopId(shopId);
            Set<Long> shopProductIds = new HashSet<>();
            for (Product p : shopProducts) {
                shopProductIds.add(p.getId());
            }

            List<Order> allOrders = orderRepository.findAll();

            long totalOrders = 0;
            long newOrders = 0;
            long confirmedOrders = 0;
            long preparingOrders = 0;
            long readyOrders = 0;
            long deliveringOrders = 0;
            long deliveredOrders = 0;
            long failedOrders = 0;
            long cancelledOrders = 0;
            long returnedOrders = 0; // Thêm biến đếm
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Order order : allOrders) {
                boolean hasShopProduct = false;
                BigDecimal orderVendorTotal = BigDecimal.ZERO;

                for (OrderDetail detail : order.getOrderDetails()) {
                    if (detail.getProduct() != null
                            && shopProductIds.contains(detail.getProduct().getId())) {
                        hasShopProduct = true;
                        BigDecimal itemTotal = detail.getPrice()
                                .multiply(new BigDecimal(detail.getQuantity()));
                        orderVendorTotal = orderVendorTotal.add(itemTotal);
                    }
                }

                if (!hasShopProduct) continue;

                totalOrders++;
                String status = order.getStatus();

                switch (status) {
                    case "NEW": newOrders++; break;
                    case "CONFIRMED": confirmedOrders++; break;
                    case "PREPARING": preparingOrders++; break;
                    case "READY_FOR_SHIPMENT": readyOrders++; break;
                    case "DELIVERING": deliveringOrders++; break;
                    case "DELIVERED":
                        deliveredOrders++;
                        totalRevenue = totalRevenue.add(orderVendorTotal);
                        break;
                    case "FAILED_DELIVERY": failedOrders++; break;
                    case "CANCELLED": cancelledOrders++; break;
                    case "RETURNED": returnedOrders++; break; // Thêm case đếm
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", totalOrders);
            stats.put("newOrders", newOrders);
            stats.put("confirmedOrders", confirmedOrders);
            stats.put("preparingOrders", preparingOrders);
            stats.put("readyForShipmentOrders", readyOrders);
            stats.put("deliveringOrders", deliveringOrders);
            stats.put("deliveredOrders", deliveredOrders);
            stats.put("failedDeliveryOrders", failedOrders);
            stats.put("cancelledOrders", cancelledOrders);
            stats.put("returnedOrders", returnedOrders); // Thêm vào response
            stats.put("totalRevenue", totalRevenue);

            logger.info("✅ Statistics calculated for Shop {}", shopId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ Error calculating statistics:", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}