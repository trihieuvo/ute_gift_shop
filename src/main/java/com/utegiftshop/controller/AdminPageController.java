package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // URL chung đã được bảo vệ (Bước 1)
public class AdminPageController {

    // 1. Trang Dashboard (UC-Admin: Thống kê) 
    @GetMapping("/dashboard")
    public String showDashboard() {
        // Trả về file: templates/admin/dashboard.html
        return "admin/dashboard"; 
    }

    // 2. Trang Quản lý Cửa hàng [cite: 124]
    @GetMapping("/manage-shops")
    public String showShopManagementPage() {
        // Trả về file: templates/admin/manage-shops.html
        return "admin/manage-shops";
    }

    // 3. Trang Quản lý Danh mục [cite: 126]
    @GetMapping("/manage-categories")
    public String showCategoryManagementPage() {
        // Trả về file: templates/admin/manage-categories.html
        return "admin/manage-categories";
    }
    
    // 4. Trang Quản lý Tài khoản 
    @GetMapping("/manage-users")
    public String showUserManagementPage() {
        // Trả về file: templates/admin/manage-users.html
        return "admin/manage-users";
    }
     // 5. Trang Quản lý Vận chuyển
    @GetMapping("/manage-shipping")
    public String manageShipping() {
        // Đảm bảo tên file là 'manage-shipping' (không có .html)
        // và nó nằm trong thư mục 'admin/'
        return "admin/manage-shipping"; 
    }

    // 6. Trang Quản lý Khuyến mãi
    @GetMapping("/manage-promotions")
    public String managePromotions() {
        // SỬA LỖI: Trả về đúng tên file HTML của trang khuyến mãi
        return "admin/manage-promotions"; 
    }
}