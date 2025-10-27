package com.utegiftshop.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.Address;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.AddressRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressRepository addressRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<Address>> getUserAddresses() {
        return ResponseEntity.ok(addressRepository.findByUserId(getCurrentUser().getId()));
    }

    @PostMapping
    public ResponseEntity<Address> addAddress(@RequestBody Address address) {
        UserDetailsImpl currentUser = getCurrentUser();
        User user = new User();
        user.setId(currentUser.getId());
        address.setUser(user);

        // Logic: Địa chỉ đầu tiên sẽ là địa chỉ mặc định
        List<Address> userAddresses = addressRepository.findByUserId(currentUser.getId());
        if (userAddresses.isEmpty()) {
            address.setDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        return ResponseEntity.ok(savedAddress);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId) {
        Long userId = getCurrentUser().getId();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
        
        // Security check
        if (!address.getUser().getId().equals(userId)) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied.");
        }
        
       // Logic: Không thể xóa địa chỉ mặc định [cite: 345]
        if (address.isDefault()) {
            return ResponseEntity.badRequest().body("Không thể xóa địa chỉ mặc định. Vui lòng chọn một địa chỉ khác làm mặc định trước.");
        }

        addressRepository.deleteById(addressId);
        return ResponseEntity.ok("Xóa địa chỉ thành công.");
    }

    @PutMapping("/{addressId}/set-default")
    @Transactional
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long addressId) {
        Long userId = getCurrentUser().getId();
        
        // 1. Bỏ tất cả địa chỉ hiện tại khỏi trạng thái mặc định
        List<Address> userAddresses = addressRepository.findByUserId(userId);
        userAddresses.forEach(addr -> addr.setDefault(false));
        addressRepository.saveAll(userAddresses);

        // 2. Đặt địa chỉ được chỉ định làm mặc định
        Address newDefaultAddress = userAddresses.stream()
            .filter(addr -> addr.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
            
        newDefaultAddress.setDefault(true);
        addressRepository.save(newDefaultAddress);
        
        return ResponseEntity.ok("Đã đặt địa chỉ làm mặc định thành công.");
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable Long addressId, @RequestBody Address updatedAddressInfo) {
        Long userId = getCurrentUser().getId();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
        
        // Security check: Đảm bảo địa chỉ này thuộc về người dùng đang đăng nhập
        if (!address.getUser().getId().equals(userId)) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied.");
        }

        // Cập nhật thông tin
        address.setRecipientName(updatedAddressInfo.getRecipientName());
        address.setPhoneNumber(updatedAddressInfo.getPhoneNumber());
        address.setFullAddress(updatedAddressInfo.getFullAddress());

        addressRepository.save(address);
        
        return ResponseEntity.ok("Cập nhật địa chỉ thành công.");
    }
}