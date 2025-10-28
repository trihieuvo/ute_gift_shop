package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Cần thiết cho hàm orderDetail
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    // ===========================================
    // 1. DASHBOARD & THỐNG KÊ
    // ===========================================
    
    @GetMapping("/dashboard")
    public String showDashboard() {
        return "admin/dashboard"; // templates/admin/dashboard.html
    }

    // ===========================================
    // 2. QUẢN LÝ NGHIỆP VỤ & CẤU HÌNH
    // ===========================================

    // --- Quản lý Cửa hàng ---
    
    // a) Trang Duyệt/Từ chối Cửa hàng mới (Shop PENDING)
    @GetMapping("/manage-shops") 
    public String showPendingShopManagementPage() {
        return "admin/manage-shops"; // templates/admin/manage-shops.html
    }
    
    // b) Trang Quản lý Chiết khấu & Shop đang hoạt động (Shop ACTIVE)
    @GetMapping("/manage-commissions")
    public String manageCommissionsPage() {
        return "admin/manage-commissions"; // templates/admin/manage-commissions.html
    }

    // --- Quản lý Sản phẩm ---
    
    // Trang Kiểm duyệt & Quản lý Sản phẩm
    @GetMapping("/manage-products") 
    public String manageProducts() {
        return "admin/manage-products"; // templates/admin/manage-products.html
    }
    
    // --- Quản lý Hệ thống ---

    // Trang Quản lý Tài khoản (User, Vendor, Shipper)
    @GetMapping("/manage-users")
    public String showUserManagementPage() {
        return "admin/manage-users"; // templates/admin/manage-users.html
    }
    
    // Trang Quản lý Danh mục
    @GetMapping("/manage-categories")
    public String showCategoryManagementPage() {
        return "admin/manage-categories"; // templates/admin/manage-categories.html
    }

    // Trang Quản lý Vận chuyển
    @GetMapping("/manage-shipping")
    public String manageShipping() {
        return "admin/manage-shipping"; // templates/admin/manage-shipping.html
    }

    // Trang Quản lý Khuyến mãi Toàn trang
    @GetMapping("/manage-promotions")
    public String managePromotions() {
        return "admin/manage-promotions"; // templates/admin/manage-promotions.html
    }

    // ===========================================
    // 3. QUẢN LÝ ĐƠN HÀNG
    // ===========================================

    // Trang Danh sách Đơn hàng
    @GetMapping("/manage-orders") 
    public String manageOrders() {
        return "admin/manage-orders"; // templates/admin/manage-orders.html
    }

    // Trang Chi tiết Đơn hàng (có ID)
    @GetMapping("/order-detail/{orderId}") 
    public String orderDetail(Model model, @PathVariable Long orderId) { 
        return "admin/order-detail"; // templates/admin/order-detail.html
    }
}