package com.utegiftshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utegiftshop.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
}