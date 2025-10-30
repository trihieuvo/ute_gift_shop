# UTE Gift Shop - Website thương mại điện tử đa nhà cung cấp

Đây là dự án website thương mại điện tử (e-commerce) được xây dựng trên nền tảng Spring Boot, mô phỏng một sàn giao dịch cho phép nhiều nhà cung cấp (Vendor) đăng bán sản phẩm, với các vai trò quản lý (Admin) và vận chuyển (Shipper) riêng biệt.

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-PostgreSQL-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)


---
## 📚 Mục Lục

* [✨ Tính năng Nổi bật ](#-tính-năng-nổi-bật)  
  * [ Khách hàng (Customer)](#-khách-hàng-customer)  
  * [ Nhà cung cấp (Vendor)](#-nhà-cung-cấp-vendor)  
  * [ Người vận chuyển (Shipper)](#-người-vận-chuyển-shipper)  
  * [ Quản trị viên (Admin)](#-quản-trị-viên-admin)  
* [ Công nghệ sử dụng](#️-công-nghệ-sử-dụng)  
* [ Bắt Đầu (Cài đặt)](#-bắt-đầu-cài-đặt)  
  * [Yêu Cầu](#yêu-cầu)  
  * [Cài Đặt](#cài-đặt)  

---
## ✨ Tính năng nổi bật 

Dự án được xây dựng với 5 vai trò (Guest, Customer, Vendor, Shipper, Admin), mỗi vai trò có một bộ chức năng chuyên biệt.

### Khách hàng (Customer)

* **Xác thực:** Đăng ký (kích hoạt qua OTP Email), Đăng nhập (JWT), Quên mật khẩu (OTP Email).  
* **Đăng ký vai trò:**  
  * Đăng ký trở thành người bán hàng (Vendor).  
  * Đăng ký trở thành Shipper.  
* **Mua sắm:**  
  * Xem, tìm kiếm, và lọc sản phẩm (theo tên, danh mục, khoảng giá).  
  * Xem chi tiết sản phẩm, bao gồm nhiều hình ảnh và đánh giá.  
  * Quản lý giỏ hàng (thêm, sửa số lượng, xóa).  
* **Thanh toán:**  
  * Quy trình checkout (chọn địa chỉ, đơn vị vận chuyển, phương thức thanh toán).  
  * Hỗ trợ thanh toán: COD (Thu hộ khi nhận hàng) và QR code (thông qua SePay).  
  * Áp dụng mã giảm giá: từ admin (sử dụng cho toàn bộ mặt hàng) hoặc từ shop (sử dụng cho mặt hàng của shop cụ thể).  
* **Quản lý tài khoản:**  
  * Quản lý thông tin cá nhân (Họ tên, SĐT).  
  * Tải lên/cắt ảnh đại diện (Avatar upload & cropper).  
  * Quản lý sổ địa chỉ (Thêm, sửa, xóa, đặt làm mặc định).  
* **Đơn hàng:**  
  * Xem lịch sử đơn hàng.  
  * Xem chi tiết đơn hàng.  
  * Hủy đơn hàng (khi ở trạng thái "Mới" hoặc "Chờ thanh toán").  
  * Đổi phương thức thanh toán (giữa COD và QR Pay) khi đơn hàng chưa được xử lý.  
* **Tương tác:**  
  * Viết, sửa đánh giá cho các sản phẩm đã mua.  
  * Chat trực tiếp với Vendor (WebSocket).  
  * Chat với Trợ lý ảo (Gemini API).  

### Nhà cung cấp (Vendor)  

* **Quản lý Cửa hàng:**  
  * Cập nhật thông tin chi tiết cửa hàng (tên, mô tả, liên hệ, MXH).  
* **Quản lý Sản phẩm:**  
  * Thêm, Sửa, Xóa sản phẩm (CRUD).  
  * Upload nhiều hình ảnh cho sản phẩm.  
  * Quản lý tồn kho, giá cả, và trạng thái (Ẩn/Hiện).  
* **Quản lý Đơn hàng:**  
  * Xem danh sách đơn hàng liên quan đến sản phẩm của mình.  
  * Cập nhật trạng thái đơn hàng (Xác nhận, Chuẩn bị, Sẵn sàng giao).  
* **Tương tác:**  
  * Xem và **phản hồi** các đánh giá của khách hàng.  
  * Chat trực tiếp với Customer (WebSocket).  
* **Marketing (Khuyến mãi):**  
  * Tạo và quản lý mã giảm giá riêng của shop (theo %, số lượng, ngày hết hạn).  
* **Thống kê:**  
  * Xem Dashboard tổng quan (đơn mới, doanh thu, tồn kho thấp).  
  * Xem biểu đồ doanh thu chi tiết (theo ngày, tháng).  

### Người vận chuyển (Shipper)  

* **Dashboard:** Xem thống kê nhanh (đơn đang xử lý, đã giao, thất bại, tiền COD đang giữ).  
* **Quản lý Đơn hàng:**  
  * Xem danh sách đơn hàng đang xử lý (sẵn sàng giao, đang giao).  
  * Xem lịch sử đơn hàng (đã giao, đã trả) có phân trang và lọc theo ngày.  
  * Xem chi tiết đơn hàng (thông tin người nhận, sản phẩm, bản đồ).  
* **Cập nhật Trạng thái:**  
  * Cập nhật trạng thái (Bắt đầu giao, Giao thành công, Giao thất bại/Chờ trả hàng).  
  * Bắt buộc nhập lý do khi giao hàng thất bại.  

### Quản trị viên (Admin)  

* **Dashboard (Thống kê):**  
  * Xem thống kê toàn hệ thống (tổng doanh thu, hoa hồng nền tảng, người dùng mới, đơn hàng).  
  * Xem biểu đồ doanh thu/hoa hồng theo thời gian.  
* **Quản lý Duyệt:**  
  * Duyệt hoặc Từ chối các yêu cầu đăng ký Vendor.  
  * Duyệt hoặc Từ chối các yêu cầu đăng ký Shipper.  
* **Quản lý Cửa hàng:**  
  * Xem danh sách các cửa hàng đang hoạt động.  
  * Thiết lập mức phí hoa hồng (commission) cho từng cửa hàng.  
* **Quản lý Người dùng:**  
  * Xem danh sách và Khóa/Mở khóa tài khoản người dùng (Customer, Vendor, Shipper).  
* **Quản lý Đơn hàng:**  
  * Xem và lọc tất cả đơn hàng trên hệ thống.  
  * **Gán đơn hàng** (trạng thái "Sẵn sàng giao") cho Shipper cụ thể.  
* **Quản lý Hệ thống:**  
  * CRUD (Thêm, Sửa, Xóa) Danh mục sản phẩm.  
  * CRUD Đơn vị vận chuyển và phí.  
  * CRUD Mã giảm giá cho toàn web.  

---
## 🛠️ Công nghệ sử dụng  

* **Backend:** Java 21, Spring Boot 3.5.7 (Web, Data JPA, Security, WebSocket)  
* **Frontend:** Thymeleaf, Bootstrap 5, JavaScript (Fetch API, DOM)  
* **Bảo mật:** Spring Security, JSON Web Tokens (JWT)  
* **Cơ sở dữ liệu:** PostgreSQL  
* **Gửi mail:** Spring Boot Mail (cho OTP)  
* **Chatbot:** Google Gemini API  
* **Build:** Apache Maven  

---
## Bắt Đầu (Cài đặt)  

Để chạy dự án này trên máy cục bộ của bạn, hãy làm theo các bước sau:

### Yêu Cầu

* Java JDK 21
* Maven 3.9+
* Một CSDL PostgreSQL đang chạy.
* Một tài khoản Gmail có "Mật khẩu ứng dụng" (App Password) để gửi mail OTP.
* API Key cho Google Gemini (để chạy Chatbot).

### Cài Đặt

1.  **Clone repository**    
```bash
git clone [URL_REPOSITORY_CỦA_BẠN]
cd ute_gift_shop
```

2.  **Cấu hình CSDL và Dịch vụ**  
* Mở file `src/main/resources/application.properties`.  
* Tạo một CSDL PostgreSQL (ví dụ: `ute_gift_shop`).  
* Cập nhật các thông tin sau:  
```properties
# Cấu hình CSDL PostgreSQL  
spring.datasource.url=jdbc:postgresql://localhost:5432/[TÊN_DATABASE_CỦA_BẠN]  
spring.datasource.username=[USERNAME_POSTGRES]  
spring.datasource.password=[PASSWORD_POSTGRES]  

# Cấu hình Email (dùng App Password của Google)  
spring.mail.username=[EMAIL_GỬI_OTP_CỦA_BẠN]@gmail.com  
spring.mail.password=[MẬT_KHẨU_ỨNG_DỤNG_16_CHỮ]  

# Cấu hình thư mục lưu ảnh  
# (Đảm bảo thư mục này tồn tại và có quyền ghi)  
app.upload.dir=./src/main/resources/static/images/  

# Cấu hình Chatbot  
google.api.key=[API_KEY_GEMINI_CỦA_BẠN]  
```

3.  **Tạo các vai trò (Roles) trong CSDL**  
* Khi chạy lần đầu, bảng `roles` sẽ được tạo. Bạn cần thêm các vai trò sau vào bảng `roles` theo thứ tự:  
  1.  `Customer`  
  2.  `Admin`  
  3.  `Vendor`  
  4.  `Shipper`  

4.  **Chạy ứng dụng**  
Sử dụng Maven wrapper (khuyến nghị):  
```bash
./mvnw spring-boot:run
```
Hoặc nếu bạn đã cài Maven:  
```bash
mvn spring-boot:run
```

5.  Ứng dụng sẽ chạy tại `http://localhost:8080`.  