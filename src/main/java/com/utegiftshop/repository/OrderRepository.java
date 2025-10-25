package com.utegiftshop.repository;

import com.utegiftshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm các đơn hàng được gán cho một shipper cụ thể VÀ có trạng thái nằm trong danh sách chỉ định.
     * @param shipperId ID của Shipper
     * @param statuses Danh sách các trạng thái (e.g., "CONFIRMED", "PREPARING")
     * @return Danh sách các đơn hàng phù hợp
     */
    List<Order> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses);

    /**
     * Đếm số lượng đơn hàng của một shipper theo một trạng thái cụ thể.
     * @param shipperId ID của Shipper
     * @param status Trạng thái đơn hàng (e.g., "DELIVERED")
     * @return Số lượng đơn hàng
     */
    long countByShipperIdAndStatus(Long shipperId, String status);

    /**
     * Đếm số lượng đơn hàng của một shipper có trạng thái nằm trong danh sách chỉ định.
     * @param shipperId ID của Shipper
     * @param statuses Danh sách các trạng thái
     * @return Số lượng đơn hàng
     */
    long countByShipperIdAndStatusIn(Long shipperId, List<String> statuses);

    // Bạn có thể thêm các phương thức khác nếu cần, ví dụ:
    // List<Order> findByShipperId(Long shipperId); // Lấy tất cả đơn của shipper
}