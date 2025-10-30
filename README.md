🎁 UTE Gift Shop - Website Thương mại Điện tử Đa Nhà cung cấp
Đây là dự án website thương mại điện tử (e-commerce) được xây dựng trên nền tảng Spring Boot, mô phỏng một sàn giao dịch cho phép nhiều nhà cung cấp (Vendor) đăng bán sản phẩm, với các vai trò quản lý (Admin) và vận chuyển (Shipper) riêng biệt.

✨ Tính năng Nổi bật
Dự án được xây dựng với 4 vai trò chính: Customer, Vendor, Shipper, và Admin, mỗi vai trò có một bộ chức năng chuyên biệt.

👤 Khách hàng (Customer)
Xác thực: Đăng ký (kích hoạt qua OTP Email), Đăng nhập (JWT), Quên mật khẩu (OTP Email).
Đăng ký vai trò:
Đăng ký của hàng (Vendor)
Đăng ký shipper

Mua sắm:

Xem, tìm kiếm, và lọc sản phẩm (theo tên, danh mục, khoảng giá).

Xem chi tiết sản phẩm, bao gồm nhiều hình ảnh và đánh giá.

Quản lý giỏ hàng (thêm, sửa số lượng, xóa).

Thanh toán:

Quy trình Checkout (chọn địa chỉ, đơn vị vận chuyển, PTTT).

Hỗ trợ thanh toán: COD (Thu hộ khi nhận hàng) và QR Code (SePay).

Áp dụng mã giảm giá (từ Admin hoặc từ Shop).

Quản lý Tài khoản:

Quản lý thông tin cá nhân (Họ tên, SĐT).

Tải lên/Cắt ảnh đại diện (Avatar upload & cropper).

Quản lý sổ địa chỉ (Thêm, Sửa, Xóa, Đặt làm mặc định).

Đơn hàng:

Xem lịch sử đơn hàng.

Xem chi tiết đơn hàng.

Hủy đơn hàng (khi ở trạng thái "Mới" hoặc "Chờ thanh toán").

Đổi phương thức thanh toán (giữa COD và QR) khi đơn hàng chưa được xử lý.

Tương tác:

Viết, sửa, xóa đánh giá cho các sản phẩm đã mua.

Chat trực tiếp với Vendor (WebSocket).

Chat với Trợ lý ảo (Gemini API).

Nâng cấp: Nộp đơn đăng ký để trở thành Vendor hoặc Shipper.

🏪 Nhà cung cấp (Vendor)
Quản lý Cửa hàng:

Cập nhật thông tin chi tiết cửa hàng (tên, mô tả, liên hệ, MXH).

Quản lý Sản phẩm:

Thêm, Sửa, Xóa sản phẩm.

Upload nhiều hình ảnh cho sản phẩm.

Quản lý tồn kho, giá cả, và trạng thái (Ẩn/Hiện).

Quản lý Đơn hàng:

Xem danh sách đơn hàng liên quan đến sản phẩm của mình.

Cập nhật trạng thái đơn hàng (Xác nhận, Chuẩn bị, Sẵn sàng giao).

Tương tác:

Xem và phản hồi các đánh giá của khách hàng.

Chat trực tiếp với Customer (WebSocket).

Marketing: Tạo và quản lý mã giảm giá riêng của Shop (theo %, số lượng, ngày hết hạn).

Thống kê:

Xem Dashboard tổng quan (đơn mới, doanh thu, tồn kho thấp).

Xem biểu đồ doanh thu chi tiết (theo ngày, tháng).

🚚 Người vận chuyển (Shipper)
Dashboard: Xem thống kê nhanh (đơn đang xử lý, đã giao, thất bại, tiền COD đang giữ).

Quản lý Đơn hàng:

Xem danh sách đơn hàng đang xử lý (sẵn sàng giao, đang giao).

Xem lịch sử đơn hàng (đã giao, đã trả) có phân trang và lọc theo ngày.

Xem chi tiết đơn hàng (thông tin người nhận, sản phẩm, bản đồ).

Cập nhật Trạng thái:

Cập nhật trạng thái (Bắt đầu giao, Giao thành công, Giao thất bại/Chờ trả hàng).

Bắt buộc nhập lý do khi giao hàng thất bại.

👑 Quản trị viên (Admin)
Dashboard: Xem thống kê toàn hệ thống (tổng doanh thu, hoa hồng, người dùng mới, đơn hàng).

Quản lý Duyệt:

Duyệt hoặc Từ chối các yêu cầu đăng ký Vendor.

Duyệt hoặc Từ chối các yêu cầu đăng ký Shipper.

Quản lý Cửa hàng:

Xem danh sách các cửa hàng đang hoạt động.

Thiết lập mức phí hoa hồng (commission) cho từng cửa hàng.

Quản lý Người dùng: Xem danh sách và Khóa/Mở khóa tài khoản người dùng.

Quản lý Đơn hàng:

Xem và lọc tất cả đơn hàng trên hệ thống.

Gán đơn hàng (trạng thái "Sẵn sàng giao") cho Shipper cụ thể.

Quản lý Hệ thống:

CRUD (Thêm, Sửa, Xóa) Danh mục sản phẩm.

CRUD Đơn vị vận chuyển và phí.

CRUD Mã giảm giá toàn hệ thống.

🛠️ Công nghệ sử dụng
Backend: Java, Spring Boot (Web, Data JPA, Security, WebSocket)

Frontend: Thymeleaf, Bootstrap 5, JavaScript (Fetch API, DOM)

Bảo mật: Spring Security, JSON Web Tokens (JWT)

Cơ sở dữ liệu: PostgreSQL (cấu hình trong application.properties)

Gửi mail: Spring Boot Mail (cho OTP)

Chatbot: Google Gemini API

Build: Apache Maven
