package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductViewController {

    @GetMapping("/products/{id}")
    public String showProductDetailPage(@PathVariable Long id, Model model) {
        // Truyền productId sang cho template để JavaScript có thể sử dụng
        model.addAttribute("productId", id);
        return "product-detail"; // Trả về file templates/product-detail.html
    }
}