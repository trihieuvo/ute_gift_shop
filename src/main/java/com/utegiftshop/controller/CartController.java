package com.utegiftshop.controller;

import com.utegiftshop.dto.request.AddToCartRequest;
import com.utegiftshop.dto.request.UpdateCartRequest;
import com.utegiftshop.entity.CartItem;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.CartItemRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCartItems() {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(cartItemRepository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequest request) {
        Long userId = getCurrentUser().getId();
        Long productId = request.getProductId();
        Integer quantity = request.getQuantity();

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại.");
        }
        Product product = productOpt.get();

        if (product.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body("Số lượng tồn kho không đủ.");
        }

        Optional<CartItem> existingCartItemOpt = cartItemRepository.findByUserIdAndProductId(userId, productId);

        if (existingCartItemOpt.isPresent()) {
            CartItem cartItem = existingCartItemOpt.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItemRepository.save(cartItem);
        } else {
            CartItem newCartItem = new CartItem();
            User user = userRepository.findById(userId).orElseThrow();
            newCartItem.setUser(user);
            newCartItem.setProduct(product);
            newCartItem.setQuantity(quantity);
            cartItemRepository.save(newCartItem);
        }
        return ResponseEntity.ok("Thêm vào giỏ hàng thành công.");
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long productId, @RequestBody UpdateCartRequest request) {
        Long userId = getCurrentUser().getId();
        Integer quantity = request.getQuantity();

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
            return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng.");
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
            return ResponseEntity.ok(cartItem);
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long productId) {
        Long userId = getCurrentUser().getId();
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        cartItemRepository.delete(cartItem);
        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng.");
    }
}