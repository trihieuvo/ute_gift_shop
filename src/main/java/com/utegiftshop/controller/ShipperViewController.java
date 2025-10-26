package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shipper")
public class ShipperViewController {

    @GetMapping("/dashboard")
    public String showDashboard() {
        return "shipper/shipper_dashboard"; // Trỏ tới templates/shipper/shipper_dashboard.html
    }

    @GetMapping("/orders")
    public String showOrdersPage() {
        return "shipper/shipper_orders"; // Trỏ tới templates/shipper/shipper_orders.html
    }

    @GetMapping("/orders/{orderId}")
    public String showOrderDetailsPage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId); // Truyền orderId sang template
        return "shipper/shipper_order_details"; // Trỏ tới templates/shipper/shipper_order_details.html
    }
}