# EXECUTIVE CONSTRUCT-CRM | Hệ Thống Quản Trị & Chăm Sóc Khách Hàng Chiến Lược 5 Khối SBU

Hệ thống CRM tinh gọn cấp điều hành dành riêng cho **Ban Lãnh Đạo (Chủ Tịch & TGĐ)** và **5 Giám Đốc Kinh Doanh** của 5 khối chiến lược (SBUs):

1. **SBU 1 - Nền móng và Hầm** (Tường vây Barrette, cọc móng sâu, tầng hầm đô thị).
2. **SBU 2 - Xây dựng Năng lượng và công nghiệp** (Tổ hợp hóa dầu, kho cảng LNG, điện gió, nhà máy công nghệ cao FDI).
3. **SBU 3 - Metro và ngầm đô thị** (Tuyến MRT, ga ngầm trung tâm, khoan hầm TBM).
4. **SBU 4 - Hạ tầng tập trung và đường sắt cao tốc** (Đường sắt tốc độ cao Bắc - Nam, cảng HKQT Long Thành, cao tốc trục dọc).
5. **SBU 5 - Cảng biển và biến đổi khí hậu** (Cảng nước sâu Cái Mép, Lạch Huyện, đê kè chắn sóng, cống kiểm soát triều).

---

## 👥 Hệ Thống Tài Khoản & Phân Quyền (RBAC)

1. **👑 Admin - Ban Lãnh Đạo (Chủ Tịch HĐQT & Tổng Giám Đốc)**:
   - Toàn quyền xem và điều hành cả 5 Khối SBU.
   - Bảng **Ma Trận Hiệu Quả 5 SBU**: So sánh doanh thu hợp đồng, tỷ lệ thu hồi dòng tiền và quy mô phễu thầu giữa các SBU.
   - Bộ lọc linh hoạt chuyển đổi xem Toàn tập đoàn hoặc chi tiết từng SBU.
2. **💼 Member - 5 Giám Đốc Kinh Doanh SBU**:
   - `gdkd_sbu1`: Giám Đốc KD SBU 1 (Nền móng & Hầm) - KS. Đỗ Hoàng Long
   - `gdkd_sbu2`: Giám Đốc KD SBU 2 (Năng lượng & Công nghiệp) - ThS. Nguyễn Quốc Thái
   - `gdkd_sbu3`: Giám Đốc KD SBU 3 (Metro & Ngầm đô thị) - KS. Vũ Trọng Khôi
   - `gdkd_sbu4`: Giám Đốc KD SBU 4 (Hạ tầng tập trung & ĐS cao tốc) - ThS. Lê Thành Trung
   - `gdkd_sbu5`: Giám Đốc KD SBU 5 (Cảng biển & BĐKH) - KS. Trần Đình Bách

*(Chuyển đổi vai trò nhanh chóng ngay tại góc trên cùng bên phải giao diện để kiểm tra góc nhìn của từng vị trí)*.

---

## 🎯 5 Module Cốt Lõi Tinh Gọn

1. **📊 1. Tổng Quan Điều Hành (Executive Dashboard)**:
   - Báo cáo 4 chỉ số tài chính vĩ mô: Tổng giá trị hợp đồng, Đã thu hồi dòng tiền, Mốc cần giải ngân, Quy mô phễu thầu đang theo đuổi.
   - Bảng Ma trận so sánh 5 Khối SBU.
   - Cảnh báo các mốc giải ngân lớn sắp tới cần thu hồi từ Chủ đầu tư.
   - Danh sách Lãnh đạo cấp cao (Chủ tịch/TGĐ đối tác) có sinh nhật hoặc ngày thành lập doanh nghiệp trong 60 ngày tới.
2. **🤝 2. Quản Lý Khách Hàng Chiến Lược (Strategic CRM 360°)**:
   - Danh bạ đối tác chiến lược B2B, B2G, FDI phân bổ theo 5 SBU.
   - Hồ sơ Lãnh đạo then chốt (Key Decision Maker): Họ tên, chức danh, SĐT trực tiếp, ngày sinh, ngày thành lập tập đoàn, mức độ thân thiết (⭐ 1-5 sao).
   - Hồ sơ 360°: Liên kết toàn bộ hợp đồng, cơ hội thầu, nhật ký gặp gỡ ngoại giao và tin nhắn Zalo đã gửi.
3. **🎯 3. Phễu Cơ Hội & Hồ Sơ Dự Thầu (Bidding Pipeline)**:
   - Kanban 6 giai đoạn rõ ràng:
     1. Tiếp cận thông tin sơ bộ
     2. Khảo sát & Đánh giá năng lực
     3. Lập hồ sơ thầu / Báo giá
     4. Thương thảo hợp đồng
     5. Trúng thầu (Ký HĐ)
     6. Trượt thầu / Tạm dừng
   - Hiển thị SBU badge, giá trị gói thầu (tỷ VNĐ), xác suất trúng thầu %.
4. **🏗️ 4. Theo Dõi Dự Án & Dòng Tiền Vĩ Mô (Project & Cashflow Tracking)**:
   - Giữ ở cấp độ Doanh nghiệp: Tên công trình, SBU, Chủ đầu tư, Giá trị HĐ, Đã giải ngân, % Tiến độ, Tình trạng sức khỏe dự án (🟢 Tốt, 🟡 Cần lưu ý).
   - Quản lý các mốc nghiệm thu & dòng tiền giải ngân. Xác nhận thu hồi dòng tiền trực tiếp.
5. **🥂 5. Chăm Sóc Khách Hàng & Ngoại Giao Lãnh Đạo (Executive Care & Zalo Automation)**:
   - Ghi nhận nhật ký gặp gỡ ngoại giao cấp cao (Bữa tối thân mật, họp giao ban chiến lược, gửi quà lễ tết, thiệp chúc mừng).
   - Bộ máy gửi tin nhắn Zalo ZNS / SMS trang trọng với mẫu kịch bản chuẩn cho lãnh đạo.
   - Trình mô phỏng điện thoại Smartphone xem trước giao diện tin nhắn trước khi gửi.
   - Nút **"Quét Ngoại Giao Tự Động"** kích hoạt gửi thông điệp chúc mừng khi đến ngày sinh nhật hoặc kỷ niệm ngày truyền thống đối tác.

---

## 🚀 Khởi Động Ứng Dụng

Chạy lệnh sau tại thư mục dự án:
```powershell
python run.py --port 8088
```

- 🖥️ **Web Admin Portal**: [http://localhost:8088](http://localhost:8088)
- 📖 **Tài liệu API (Swagger UI)**: [http://localhost:8088/docs](http://localhost:8088/docs)
*(Cổng 8088 không trùng lặp với PM Lương)*.
