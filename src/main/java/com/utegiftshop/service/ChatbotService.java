// src/main/java/com/utegiftshop/service/ChatbotService.java
package com.utegiftshop.service;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors; // <-- Added this import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${google.api.key}")
    private String googleApiKey;

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    // --- KIEN_THUC_FAQ (Keep as is) ---
    private static final String KIEN_THUC_FAQ = """
            Dưới đây là các thông tin và câu hỏi thường gặp (FAQ) của UTE GiftShop. 
            Hãy dựa vào đây để trả lời các câu hỏi liên quan:
            
            **Về Tài khoản & Đăng ký:**
            - Hỏi: Làm thế nào để trở thành Shipper?
            - Đáp: Chào bạn! Hiện tại UTE GiftShop chưa có chức năng tuyển dụng shipper tự động. 
                Bạn vui lòng liên hệ ban quản trị để biết thêm chi tiết về quy trình đăng ký.
            
            - Hỏi: Làm thế nào để trở thành Nhà bán hàng (Vendor)?
            - Đáp: Tương tự như Shipper, bạn cần liên hệ ban quản trị để đăng ký mở gian hàng trên UTE GiftShop.
            
            **Về Giao hàng & Đơn hàng:**
            - Hỏi: Shop có các phương thức vận chuyển nào?
            - Đáp: Hiện tại chúng tôi có hỗ trợ 2 phương thức chính là giao hàng nhanh và giao hàng hoả tốc để đáp ứng các nhu cầu khác nhau của khách hàng
            
            - Hỏi: Tôi có thể hủy đơn hàng không?
            - Đáp: Bạn có thể tự hủy đơn hàng nếu đơn hàng đang ở trạng thái "Mới" (NEW). 
                Nếu đơn hàng đã được xác nhận, bạn vui lòng liên hệ shop để được hỗ trợ.
            """;
    // --- End KIEN_THUC_FAQ ---

    public String getChatReply(String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = """
            Bạn là trợ lý ảo của UTE GiftShop. Hãy trả lời các câu hỏi của khách hàng một cách thân thiện, bằng tiếng Việt.
            Sử dụng Markdown ĐƠN GIẢN để định dạng.
            Dùng 2 dấu sao liền nhau cho chữ đậm, VÍ DỤ: `**chữ đậm**`.
            Dùng gạch đầu dòng (`- `) hoặc số (`1. `) để liệt kê.
            QUAN TRỌNG: KHÔNG dùng backslash (\\) để escape các dấu sao.

            %s

            Hãy dựa vào thông tin FAQ ở trên để trả lời các câu hỏi liên quan.
            Nếu câu hỏi không liên quan đến FAQ hoặc mua sắm tại UTE GiftShop, hãy từ chối một cách lịch sự.
            """.formatted(KIEN_THUC_FAQ);

        Map<String, Object> textPart = Map.of("text", userMessage);
        Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(textPart));
        Map<String, Object> modelContextPart = Map.of("text", systemPrompt);
        Map<String, Object> modelContext = Map.of("role", "model", "parts", List.of(modelContextPart));

        Map<String, Object> body = Map.of(
            "contents", List.of(modelContext, userContent)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = UriComponentsBuilder.fromHttpUrl(GEMINI_API_BASE_URL + GEMINI_MODEL + ":generateContent")
                                        .queryParam("key", googleApiKey)
                                        .toUriString();

        try {
            logger.info("Sending request to Gemini API. URL (without key): {}", GEMINI_API_BASE_URL + GEMINI_MODEL + ":generateContent");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            logger.info("Received response status from Gemini: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                logger.debug("Gemini Response Body: {}", responseBody);

                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            // Concatenate text from all parts
                            return parts.stream()
                                        .map(part -> (String) part.getOrDefault("text", ""))
                                        .collect(Collectors.joining("\n")); // Now Collectors should resolve
                        } else {
                             logger.warn("Gemini response format unexpected: 'parts' array is missing or empty in content.");
                        }
                    } else {
                         logger.warn("Gemini response format unexpected: 'content' missing in candidate.");
                    }
                } else {
                    logger.warn("Gemini response format unexpected: 'candidates' array is missing or empty.");
                    if (responseBody.containsKey("promptFeedback")) {
                        logger.error("Gemini API Prompt Feedback: {}", responseBody.get("promptFeedback"));
                        return "Trợ lý AI không thể xử lý yêu cầu do vấn đề nội dung.";
                    }
                }
            } else {
                 logger.error("Gemini API returned non-OK status: {}. Body: {}", response.getStatusCode(), response.getBody());
            }
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này (lỗi phản hồi từ AI).";

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("HTTP Error connecting to Gemini API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "Đã xảy ra lỗi HTTP khi kết nối với trợ lý AI: " + e.getStatusCode();
        } catch (Exception e) {
            logger.error("Exception connecting to Gemini API:", e);
            return "Đã xảy ra lỗi kết nối với trợ lý AI.";
        }
    }
}