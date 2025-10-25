package com.utegiftshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor // Tạo constructor nhận tất cả các tham số
public class ShipperStatsDto {
    private long assigned; // Đơn đang chờ/cần giao
    private long delivered; // Đơn đã giao thành công
    private long failed; // Đơn giao thất bại
}