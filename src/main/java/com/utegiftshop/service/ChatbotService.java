package com.utegiftshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    public String getChatReply(String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); 

        // Xây dựng System Prompt (Cập nhật)
        String systemPrompt = "Bạn là trợ lý ảo của UTE GiftShop. Hãy trả lời các câu hỏi của khách hàng một cách thân thiện. " +
                              "Sử dụng Markdown ĐƠN GIẢN để định dạng. " +
                              "Dùng 2 dấu sao liền nhau cho chữ đậm, VÍ DỤ: `**chữ đậm**`. " +
                              "Dùng gạch đầu dòng (`- `) hoặc số (`1. `) để liệt kê. " +
                              "QUAN TRỌNG: KHÔNG dùng backslash (\\) để escape các dấu sao. " +
                              "Chỉ trả về `**chữ**` chứ KHÔNG trả về `\\*\\*chữ\\*\\*`. " +
                              "Không trả lời các câu hỏi không liên quan đến mua sắm hoặc cửa hàng.";


        Map<String, Object> body = Map.of(
            "model", "deepseek/deepseek-chat-v3.1", 
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            // Xử lý response (cấu trúc có thể khác nhau)
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Đã xảy ra lỗi khi kết nối với trợ lý AI.";
        }
    }
}