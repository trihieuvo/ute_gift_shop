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
        // === KẾT THÚC THAY ĐỔI ===
    public String getChatReply(String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); 

        String systemPrompt = """
                    Bạn là trợ lý ảo của UTE GiftShop. Hãy trả lời các câu hỏi của khách hàng một cách thân thiện.
                    Sử dụng Markdown ĐƠN GIẢN để định dạng.
                    Dùng 2 dấu sao liền nhau cho chữ đậm, VÍ DỤ: `**chữ đậm**`.
                    Dùng gạch đầu dòng (`- `) hoặc số (`1. `) để liệt kê.
                    QUAN TRỌNG: KHÔNG dùng backslash (\\) để escape các dấu sao.
                    
                    %s
                    
                    Hãy dựa vào thông tin FAQ ở trên để trả lời các câu hỏi liên quan.
                    Nếu câu hỏi không liên quan đến FAQ hoặc mua sắm, hãy từ chối một cách lịch sự.
                    """.formatted(KIEN_THUC_FAQ);


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