package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vendor")
public class VendorViewController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "vendor/vendor_dashboard";
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/shop")
    public String showShopManagement(Model model) {
        model.addAttribute("activePage", "shop");
        return "vendor/vendor_shop"; // <-- Update file name
    }

    @GetMapping("/products")
    public String showProductManagement(Model model) {
        model.addAttribute("activePage", "products");
        return "vendor/vendor_products";
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/orders")
    public String showOrderManagement(Model model) {
        model.addAttribute("activePage", "orders");
        return "vendor/vendor_orders"; // <-- Update file name
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/promotions")
    public String showPromotionManagement(Model model) {
         model.addAttribute("activePage", "promotions");
        return "vendor/vendor_promotions"; // <-- Update file name
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/stats")
    public String showStats(Model model) {
         model.addAttribute("activePage", "stats");
        return "vendor/vendor_stats"; // <-- Update file name
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/reviews")
    public String showReviews(Model model) {
         model.addAttribute("activePage", "reviews");
        return "vendor/vendor_reviews"; // <-- Update file name
    }

    // === UPDATE RETURN VALUE ===
    @GetMapping("/messages")
    public String showMessages(Model model) {
         model.addAttribute("activePage", "messages");
        return "vendor/vendor_messages"; // <-- Update file name
    }
    @GetMapping("/register")
    public String showRegisterShopPage(Model model) {
        model.addAttribute("activePage", "register"); // Đặt activePage nếu cần
        return "vendor/vendor_register_shop"; // Trỏ đến file HTML mới
    }
}