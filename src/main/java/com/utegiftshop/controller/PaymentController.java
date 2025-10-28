package com.utegiftshop.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping; // Import UriComponentsBuilder
import org.springframework.web.bind.annotation.RequestParam; // Import Collections
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utegiftshop.entity.Order;
import com.utegiftshop.repository.OrderRepository;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/check-status")
    @Transactional
    public ResponseEntity<?> checkPaymentStatus(@RequestParam("code") String paymentCode) {
        logger.info("Checking payment status for code: {}", paymentCode);

        // 1. Tìm đơn hàng
        Optional<Order> orderOpt = orderRepository.findByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            logger.warn("Order not found for payment code: {}", paymentCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("paid", false, "error", "Không tìm thấy đơn hàng với mã thanh toán này."));
        }
        Order order = orderOpt.get();

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            logger.info("Order {} already processed with status: {}", order.getId(), order.getStatus());
            return ResponseEntity.ok(Map.of("paid", true));
        }

        // 2. Gọi API SePay
        // --- !!! THAY TOKEN CỦA BẠN VÀO ĐÂY !!! ---
        String apiToken = "HL1TIEXVOABXCTRDJFHOYNRJZULNZKC4IJTZQDSZM5B7NVQGSSPOY0W26MKEPWMU";
        // ------------------------------------------

        // --- CẬP NHẬT URL API ---
        String sepayUrl = UriComponentsBuilder.fromHttpUrl("https://my.sepay.vn/userapi/transactions/list")
                .queryParam("since_id", 20) // Lấy 20 giao dịch gần nhất
                .toUriString();
        // -------------------------

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON)); // Sửa Accept header
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            logger.debug("Calling SePay API: {}", sepayUrl);
            ResponseEntity<String> response = restTemplate.exchange(sepayUrl, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();
            logger.debug("SePay API Response Body: {}", responseBody);

            if (responseBody != null && response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> sepayResponse = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                Object transactionsObj = sepayResponse.get("transactions");

                if (transactionsObj instanceof List) {
                    List<Map<String, Object>> transactions = (List<Map<String, Object>>) transactionsObj;
                    logger.debug("Found {} transactions in response.", transactions.size());

                    for (Map<String, Object> tx : transactions) {
                        // --- SỬA TÊN TRƯỜNG ---
                        String transactionContent = (String) tx.get("transaction_content");
                        // -----------------------

                        // Kiểm tra xem transaction_content có chứa paymentCode không
                        if (transactionContent != null && transactionContent.contains(paymentCode)) {
                            logger.info("Payment found for code: {}! Transaction ID: {}", paymentCode, tx.get("id"));

                            // Cập nhật trạng thái đơn hàng
                            order.setStatus("CONFIRMED"); // Đã xác nhận thanh toán
                            order.setPaymentStatus("SUCCESS");
                            order.setPaymentTransId(String.valueOf(tx.get("id")));
                            orderRepository.save(order);
                            logger.info("Order {} status updated to CONFIRMED.", order.getId());

                            return ResponseEntity.ok(Map.of("paid", true));
                        }
                    }
                    logger.debug("Payment code {} not found in the latest transactions list.", paymentCode);
                } else {
                    logger.warn("SePay response does not contain a valid 'transactions' list or it's not a list.");
                }
            } else {
                logger.error("SePay API call failed with status: {}. Body: {}", response.getStatusCode(), responseBody);
                return ResponseEntity.status(response.getStatusCode()).body(Map.of("paid", false, "error", "Lỗi khi gọi API SePay. Status: " + response.getStatusCode()));
            }
        } catch (HttpClientErrorException e) {
            logger.error("HttpClientError when calling SePay API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("paid", false, "error", "Lỗi từ SePay: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            logger.error("Error processing SePay response or updating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("paid", false, "error", "Lỗi server khi kiểm tra thanh toán: " + e.getMessage()));
        }

        // 4. Không tìm thấy giao dịch nào khớp
        return ResponseEntity.ok(Map.of("paid", false));
    }
}

