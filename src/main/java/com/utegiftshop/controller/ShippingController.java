package com.utegiftshop.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.ShippingMethod;
import com.utegiftshop.repository.ShippingMethodRepository;

@RestController
@RequestMapping("/api/shipping-methods")
public class ShippingController {

    @Autowired
    private ShippingMethodRepository shippingMethodRepository;

    @GetMapping
    public ResponseEntity<List<ShippingMethod>> getAllShippingMethods() {
        return ResponseEntity.ok(shippingMethodRepository.findAll());
    }
}