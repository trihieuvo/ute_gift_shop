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

    private static final String KIEN_THUC_FAQ = """
        Dưới đây là các thông tin và câu hỏi thường gặp (FAQ) về UTE GiftShop.
        Hãy dựa vào đây để trả lời các câu hỏi của khách hàng một cách chi tiết và chính xác.

        **1. Tài khoản & Đăng nhập:**

        - **Hỏi:** Làm sao để tạo tài khoản mới?
        - **Đáp:** Bạn có thể tạo tài khoản bằng cách nhấn vào nút **Đăng ký**. Điền đầy đủ **Họ tên**, **Email**, và **Mật khẩu** (nhập lại mật khẩu để xác nhận) rồi nhấn nút **Đăng ký**. Sau đó, bạn cần kiểm tra email để nhận mã **OTP** và **kích hoạt** tài khoản.

        - **Hỏi:** Tôi không nhận được email kích hoạt OTP?
        - **Đáp:** Vui lòng kiểm tra hộp thư **Spam** hoặc **Thư rác**. Nếu vẫn không thấy, có thể có lỗi trong quá trình gửi email. Bạn thử đăng ký lại hoặc liên hệ hỗ trợ. Mã OTP có hiệu lực trong **10 phút**.

        - **Hỏi:** Làm thế nào để đăng nhập?
        - **Đáp:** Truy cập trang **Đăng nhập**, nhập **Email** và **Mật khẩu** của bạn, sau đó nhấn nút **Đăng nhập**. Đảm bảo tài khoản của bạn đã được **kích hoạt**.

        - **Hỏi:** Tôi quên mật khẩu thì phải làm sao?
        - **Đáp:** Bạn có thể nhấn vào liên kết **Quên mật khẩu?** trên trang Đăng nhập. Nhập **email** đã đăng ký và nhấn **Gửi mã OTP**. Sau đó, nhập mã **OTP** nhận được qua email và **mật khẩu mới** để đặt lại mật khẩu. Mã OTP này cũng có hiệu lực **10 phút**.

        - **Hỏi:** Làm thế nào để thay đổi thông tin cá nhân (Họ tên, SĐT, Avatar)?
        - **Đáp:** Đăng nhập vào tài khoản, truy cập trang **Tài khoản của tôi** (Profile), chọn tab **Hồ sơ của tôi**. Tại đây bạn có thể cập nhật **Họ tên**, **Số điện thoại** và nhấn **Lưu thay đổi**. Để đổi **avatar**, nhấn vào ảnh đại diện hiện tại, chọn ảnh mới, cắt ảnh nếu cần và xác nhận tải lên.

        - **Hỏi:** Làm sao để đổi mật khẩu khi đã đăng nhập?
        - **Đáp:** Vào trang **Tài khoản của tôi**, chọn tab **Đổi mật khẩu**. Nhập **Mật khẩu hiện tại**, **Mật khẩu mới** và **Xác nhận mật khẩu mới**, sau đó nhấn **Xác nhận**.

        **2. Sản phẩm & Mua sắm:**

        - **Hỏi:** Làm thế nào để tìm kiếm sản phẩm?
        - **Đáp:** Bạn có thể sử dụng thanh **tìm kiếm** ở đầu trang chủ để nhập **tên sản phẩm** cần tìm. Bạn cũng có thể duyệt sản phẩm theo **Danh mục** hoặc lọc theo **Khoảng giá** ở cột bên trái trang chủ.

        - **Hỏi:** Làm sao để xem chi tiết một sản phẩm?
        - **Đáp:** Nhấn vào **hình ảnh** hoặc **tên sản phẩm** hoặc nút **Xem chi tiết** trên trang danh sách sản phẩm để đến trang chi tiết. Tại đây bạn có thể xem **hình ảnh**, **mô tả**, **giá**, và các **đánh giá** (nếu có).

        - **Hỏi:** Làm thế nào để thêm sản phẩm vào giỏ hàng?
        - **Đáp:** Trên trang chi tiết sản phẩm, chọn **số lượng** mong muốn và nhấn nút **Thêm vào giỏ hàng**. Bạn cần **đăng nhập** để thực hiện thao tác này.

        **3. Giỏ hàng & Thanh toán:**

        - **Hỏi:** Làm sao để xem giỏ hàng?
        - **Đáp:** Nhấn vào biểu tượng **Giỏ hàng** ở góc trên bên phải màn hình hoặc truy cập trực tiếp trang **Giỏ hàng** (/cart).

        - **Hỏi:** Tôi có thể thay đổi số lượng hoặc xóa sản phẩm trong giỏ hàng không?
        - **Đáp:** Có. Tại trang **Giỏ hàng**, bạn có thể thay đổi **số lượng** trong ô nhập liệu. Nếu bạn nhập số lượng là **0** hoặc nhấn vào biểu tượng **thùng rác**, hệ thống sẽ hỏi xác nhận **xóa** sản phẩm đó khỏi giỏ.

        - **Hỏi:** Làm thế nào để tiến hành thanh toán?
        - **Đáp:** Sau khi kiểm tra giỏ hàng, nhấn nút **Tiến hành thanh toán**. Bạn sẽ được chuyển đến trang **Thanh toán**.

        - **Hỏi:** Các bước thanh toán như thế nào?
        - **Đáp:** Tại trang Thanh toán, bạn cần:
            1.  **Chọn địa chỉ giao hàng** từ danh sách địa chỉ đã lưu hoặc thêm địa chỉ mới.
            2.  **Chọn phương thức thanh toán** (Hiện tại chỉ hỗ trợ **COD - Thanh toán khi nhận hàng**).
            3.  Kiểm tra lại **Tóm tắt đơn hàng**.
            4.  Nhấn nút **Đặt hàng**.

        - **Hỏi:** Shop có những phương thức thanh toán nào?
        - **Đáp:** Hiện tại, UTE GiftShop hỗ trợ phương thức **Thanh toán khi nhận hàng (COD)**.

        **4. Quản lý đơn hàng & Giao hàng:**

        - **Hỏi:** Làm sao để xem lịch sử đơn hàng đã đặt?
        - **Đáp:** Đăng nhập và truy cập trang **Lịch sử đơn hàng** từ menu tài khoản hoặc vào trực tiếp `/order-history`. Tại đây bạn có thể xem danh sách các đơn hàng đã đặt và trạng thái của chúng.

        - **Hỏi:** Làm sao để xem chi tiết một đơn hàng cụ thể?
        - **Đáp:** Từ trang **Lịch sử đơn hàng**, nhấn nút **Xem chi tiết** ở đơn hàng bạn muốn xem. Trang chi tiết sẽ hiển thị danh sách **sản phẩm**, **tổng tiền**, **địa chỉ**, **phương thức thanh toán**, và **trạng thái** hiện tại của đơn hàng.

        - **Hỏi:** Tôi có thể hủy đơn hàng đã đặt không?
        - **Đáp:** Bạn **chỉ có thể** tự hủy đơn hàng nếu đơn hàng đang ở trạng thái **"Mới" (NEW)**. Tại trang **Chi tiết đơn hàng**, nếu đơn ở trạng thái "Mới", sẽ có nút **Hủy đơn hàng**. Nếu đơn hàng đã ở trạng thái khác (Đã xác nhận, Đang giao,...), bạn không thể tự hủy.

        - **Hỏi:** Các trạng thái đơn hàng có ý nghĩa gì?
        - **Đáp:**
            - **NEW:** Đơn hàng mới được tạo, chờ người bán xác nhận.
            - **CONFIRMED:** Người bán đã xác nhận đơn hàng.
            - **PREPARING:** Người bán đang chuẩn bị hàng.
            - **READY_FOR_SHIPMENT:** Hàng đã chuẩn bị xong, sẵn sàng giao cho shipper.
            - **DELIVERING:** Shipper đang giao hàng đến bạn.
            - **DELIVERED:** Giao hàng thành công.
            - **CANCELLED:** Đơn hàng đã bị hủy (bởi bạn hoặc người bán).
            - **RETURN_PENDING:** Shipper báo giao hàng thất bại, đang chờ trả hàng về cho người bán.
            - **RETURNED:** Shipper đã trả hàng thất bại về cho người bán.
        

        - **Hỏi:** Phí vận chuyển được tính như thế nào?
        - **Đáp:** Hiện tại, theo giao diện giỏ hàng và thanh toán, phí vận chuyển đang được **miễn phí**.

        **5. Sổ địa chỉ:**

        - **Hỏi:** Làm sao để quản lý địa chỉ nhận hàng?
        - **Đáp:** Vào trang **Tài khoản của tôi**, chọn tab **Sổ địa chỉ**. Tại đây bạn có thể **Thêm địa chỉ mới**, **Sửa**, **Xóa**, hoặc **Đặt làm mặc định** cho các địa chỉ đã lưu.

        - **Hỏi:** Tại sao tôi không xóa được địa chỉ mặc định?
        - **Đáp:** Hệ thống yêu cầu phải luôn có một địa chỉ mặc định. Để xóa địa chỉ đang là mặc định, bạn cần **chọn một địa chỉ khác làm mặc định trước**, sau đó mới có thể xóa địa chỉ cũ.

        **6. Đăng ký vai trò (Vendor/Shipper):**

        - **Hỏi:** Làm thế nào để đăng ký làm Người bán hàng (Vendor) hoặc Người giao hàng (Shipper)?
        - **Đáp:** Nếu bạn đang là **Customer**, hãy vào trang **Tài khoản của tôi**, chọn tab **Đăng ký vai trò**. Chọn **vai trò** bạn muốn (Vendor hoặc Shipper), có thể ghi thêm **lời nhắn/lý do**, và nhấn **Gửi yêu cầu**. Yêu cầu của bạn sẽ được gửi đến Admin để **xét duyệt**.

        - **Hỏi:** Tôi đã gửi yêu cầu đăng ký vai trò, làm sao biết kết quả?
        - **Đáp:** Hiện tại, bạn có thể kiểm tra lại tab **Đăng ký vai trò**. Nếu yêu cầu của bạn vẫn đang **chờ duyệt (PENDING)**, form đăng ký sẽ bị khóa và có thông báo hiển thị. Nếu yêu cầu được **phê duyệt (APPROVED)**, vai trò của bạn sẽ được cập nhật và bạn sẽ thấy các menu/chức năng tương ứng (ví dụ: Trang bán hàng cho Vendor, Bảng điều khiển cho Shipper). Nếu bị **từ chối (REJECTED)**, bạn có thể gửi lại yêu cầu mới.

        **7. Đánh giá sản phẩm:**

        - **Hỏi:** Khi nào tôi có thể đánh giá sản phẩm?
        - **Đáp:** Bạn chỉ có thể đánh giá một sản phẩm sau khi bạn đã **mua** sản phẩm đó và đơn hàng chứa sản phẩm đó đã được giao thành công (**DELIVERED**). Bạn cũng **chưa từng đánh giá** sản phẩm đó cho chính đơn hàng đó trước đây.

        - **Hỏi:** Làm thế nào để gửi đánh giá?
        - **Đáp:** Truy cập trang **chi tiết sản phẩm** bạn muốn đánh giá. Nếu bạn đủ điều kiện, một **khung viết đánh giá** sẽ hiện ra. Bạn cần chọn **số sao** (từ 1 đến 5), có thể viết thêm **bình luận**, và nhấn nút **Gửi đánh giá**.

        - **Hỏi:** Làm sao để xem các đánh giá của sản phẩm?
        - **Đáp:** Các đánh giá (nếu có) sẽ được hiển thị ở phần dưới của trang **chi tiết sản phẩm**.

        **8. Trò chuyện (Chat):**

        - **Hỏi:** Làm thế nào để sử dụng trợ lý ảo (chatbot)?
        - **Đáp:** Nhấn vào **biểu tượng chat** màu xanh ở góc dưới bên phải màn hình để mở cửa sổ chat. Bạn cần **đăng nhập** để sử dụng tính năng này. Nhập câu hỏi của bạn vào ô chat và nhấn **Gửi**.

        - **Hỏi:** Tôi có thể chat trực tiếp với người bán không?
        - **Đáp:** Có. Người bán hàng (Vendor) có một giao diện **Tương tác Khách hàng** riêng để xem và trả lời tin nhắn từ khách hàng thông qua hệ thống chat WebSocket. Khi bạn chat, người bán sẽ nhận được thông báo và có thể phản hồi bạn.

        **9. Dành cho Người bán (Vendor):**

        - **Hỏi:** Làm sao để truy cập trang quản lý bán hàng?
        - **Đáp:** Nếu tài khoản của bạn đã được duyệt là **Vendor**, bạn có thể truy cập **Trang bán hàng** từ menu tài khoản hoặc vào trực tiếp `/vendor/dashboard`.

        - **Hỏi:** Vendor có thể làm gì trên trang quản lý?
        - **Đáp:** Vendor có thể:
            - Xem **Tổng quan** (đơn mới, doanh thu, sản phẩm tồn kho).
            - **Quản lý thông tin cửa hàng** (Tên, Mô tả).
            - **Quản lý Sản phẩm** (Thêm, Sửa, Xóa, Tải ảnh, Đặt trạng thái Hiển thị/Ẩn).
            - **Quản lý Đơn hàng** (Xem danh sách, Xem chi tiết, Cập nhật trạng thái: Xác nhận -> Chuẩn bị -> Sẵn sàng giao).
            - **Quản lý Khuyến mãi** (Tạo, Sửa, Xóa mã giảm giá theo %, số lượng, đơn tối thiểu, thời gian áp dụng).
            - Xem **Thống kê Doanh thu** (Theo ngày, tháng, năm và biểu đồ).
            - **Quản lý Đánh giá** (Xem đánh giá sản phẩm của shop và Phản hồi đánh giá).
            - **Tương tác Khách hàng** (Chat trực tiếp với khách hàng qua WebSocket).

        **10. Dành cho Người giao hàng (Shipper):**

        - **Hỏi:** Làm sao để truy cập bảng điều khiển Shipper?
        - **Đáp:** Nếu tài khoản của bạn đã được duyệt là **Shipper**, bạn có thể truy cập **Dashboard Shipper** từ menu tài khoản hoặc vào trực tiếp `/shipper/dashboard`.

        - **Hỏi:** Shipper có thể làm gì?
        - **Đáp:** Shipper có thể:
            - Xem **Bảng điều khiển** (Số đơn đang xử lý, tiền COD đang giữ, số đơn đã giao/thất bại).
            - Xem danh sách **Đơn hàng đang xử lý** (bao gồm các đơn CONFIRMED, PREPARING, DELIVERING).
            - Xem **Lịch sử đơn hàng** (đã giao, thất bại, chờ trả, đã trả) với khả năng lọc theo ngày.
            - Xem **Chi tiết đơn hàng** (Thông tin người nhận, sản phẩm, tổng tiền, trạng thái).
            - **Cập nhật trạng thái đơn hàng**: Bắt đầu giao (DELIVERING), Giao thành công (DELIVERED - yêu cầu ảnh POD), Giao thất bại (RETURN_PENDING - yêu cầu lý do).

        """;

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