package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/manage-shops")
    public String showPendingShopManagementPage(Model model) {
        model.addAttribute("activePage", "manage-shops");
        return "admin/manage-shops";
    }

    @GetMapping("/manage-commissions")
    public String manageCommissionsPage(Model model) {
        model.addAttribute("activePage", "manage-commissions");
        return "admin/manage-commissions";
    }

    @GetMapping("/manage-products")
    public String manageProducts(Model model) {
        model.addAttribute("activePage", "manage-products");
        return "admin/manage-products";
    }

    @GetMapping("/manage-users")
    public String showUserManagementPage(Model model) {
        model.addAttribute("activePage", "manage-users");
        return "admin/manage-users";
    }

    @GetMapping("/manage-categories")
    public String showCategoryManagementPage(Model model) {
        model.addAttribute("activePage", "manage-categories");
        return "admin/manage-categories";
    }

    @GetMapping("/manage-shipping")
    public String manageShipping(Model model) {
        model.addAttribute("activePage", "manage-shipping");
        return "admin/manage-shipping";
    }

    @GetMapping("/manage-promotions")
    public String managePromotions(Model model) {
        model.addAttribute("activePage", "manage-promotions");
        return "admin/manage-promotions";
    }

    @GetMapping("/manage-orders")
    public String manageOrders(Model model) {
        model.addAttribute("activePage", "manage-orders");
        return "admin/manage-orders";
    }

    @GetMapping("/order-detail/{orderId}")
    public String orderDetail(Model model, @PathVariable Long orderId) {
        model.addAttribute("activePage", "manage-orders"); // Vẫn highlight mục "Quản lý Đơn hàng"
        return "admin/order-detail";
    }

    @GetMapping("/manage-shippers")
    public String manageShippers(Model model) {
        model.addAttribute("activePage", "manage-shippers");
        return "admin/manage-shippers";
    }
}