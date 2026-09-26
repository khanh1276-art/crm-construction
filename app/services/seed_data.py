"""
Seed realistic Executive Construction CRM data for:
- Leadership (Ban Lãnh Đạo - Admin)
- 5 SBU Business Development Directors:
  1. SBU1 - Nền móng và Hầm
  2. SBU2 - Xây dựng Năng lượng và công nghiệp
  3. SBU3 - Metro và ngầm đô thị
  4. SBU4 - Hạ tầng tập trung và đường sắt cao tốc
  5. SBU5 - Cảng biển và biến đổi khí hậu
"""
import json
from datetime import datetime, timedelta
from app.database import get_db, init_db

def seed_database():
    init_db()
    conn = get_db()
    cursor = conn.cursor()

    # Clear tables
    cursor.execute("DELETE FROM automated_messages;")
    cursor.execute("DELETE FROM customer_care_activities;")
    cursor.execute("DELETE FROM milestones;")
    cursor.execute("DELETE FROM projects;")
    cursor.execute("DELETE FROM pipeline_bids;")
    cursor.execute("DELETE FROM customers;")
    cursor.execute("DELETE FROM users;")
    try:
        cursor.execute("DELETE FROM sqlite_sequence;")
    except Exception:
        pass

    today = datetime.now()
    d_fmt = "%Y-%m-%d"

    # 1. USERS & ACCOUNTS (Admin, 5 GĐKD SBU, và Cộng Tác Viên)
    users_data = [
        # Ban Lãnh Đạo (Admin)
        ("admin", "123456", "Ban Lãnh Đạo (Chủ Tịch & TGĐ)", "ADMIN", "ALL", "Chủ Tịch HĐQT & Tổng Giám Đốc", "lanhdao@xaydung-corp.vn", "0903888999", "fa-user-shield"),
        # Giám Đốc Kinh Doanh (5 Khối SBU)
        ("gdkd_sbu1", "123456", "KS. Đỗ Hoàng Long", "SBU_DIRECTOR", "SBU1", "Giám Đốc Kinh Doanh SBU 1 (Nền móng & Hầm)", "long.do@xaydung-corp.vn", "0912111222", "fa-layer-group"),
        ("gdkd_sbu2", "123456", "ThS. Nguyễn Quốc Thái", "SBU_DIRECTOR", "SBU2", "Giám Đốc Kinh Doanh SBU 2 (Năng lượng & Công nghiệp)", "thai.nguyen@xaydung-corp.vn", "0913222333", "fa-bolt"),
        ("gdkd_sbu3", "123456", "KS. Vũ Trọng Khôi", "SBU_DIRECTOR", "SBU3", "Giám Đốc Kinh Doanh SBU 3 (Metro & Ngầm đô thị)", "khoi.vu@xaydung-corp.vn", "0914333444", "fa-train-subway"),
        ("gdkd_sbu4", "123456", "ThS. Lê Thành Trung", "SBU_DIRECTOR", "SBU4", "Giám Đốc Kinh Doanh SBU 4 (Hạ tầng & ĐS cao tốc)", "trung.le@xaydung-corp.vn", "0915444555", "fa-road"),
        ("gdkd_sbu5", "123456", "KS. Trần Đình Bách", "SBU_DIRECTOR", "SBU5", "Giám Đốc Kinh Doanh SBU 5 (Cảng biển & BĐKH)", "bach.tran@xaydung-corp.vn", "0916555666", "fa-anchor"),
        # Cộng Tác Viên (CTV)
        ("ctv_hanoi", "123456", "ThS. Hoàng Minh Đức", "COLLABORATOR", "ALL", "Cộng Tác Viên Phát Triển Dự Án B2G (Miền Bắc)", "duc.hoang@ctv-partner.vn", "0988112233", "fa-handshake"),
        ("ctv_saigon", "123456", "KS. Phạm Thị Mai Lan", "COLLABORATOR", "SBU2", "Cộng Tác Viên Kết Nối FDI & Năng Lượng", "lan.pham@ctv-partner.vn", "0977223344", "fa-handshake-angle"),
        ("ctv_mientay", "123456", "ThS. Nguyễn Văn Hải", "COLLABORATOR", "SBU5", "Cộng Tác Viên Cảng Biển & Hạ Tầng (ĐBSCL)", "hai.nguyen@ctv-partner.vn", "0966334455", "fa-people-arrows")
    ]
    cursor.executemany("""
    INSERT INTO users (username, password, full_name, role, sbu, title, email, phone, avatar_icon)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, users_data)

    # 2. STRATEGIC CUSTOMERS (Theo Chính sách CSKH FECON: DIAMOND, GOLD, SILVER)
    customers_data = [
        # SBU 1 - Nền móng và Hầm
        (
            "KH-SBU1-01", "Tập đoàn Bất động sản Masterise Homes", "SBU1", "DIAMOND", "B2B",
            "0312567890", "0283915915", "contact@masterisehomes.com", "Tòa nhà The Landmark 81, Bình Thạnh, TP.HCM",
            "Ông Phan Trọng Đạt", "Phó Tổng Giám Đốc Khối Phát Triển Dự Án", "0908112233", "1978-10-18", "2014-11-20",
            5, "EXCELLENT", "Đối tác chiến lược toàn diện mảng tường vây Barrette và cọc khoan nhồi móng sâu đại dự án trung tâm.",
            15.0, 25.0, 25.0, 20.0, 15.0, 100.0, 0, 0, 80000000.0, 15000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),
        (
            "KH-SBU1-02", "Công ty TNHH Keppel Land Việt Nam", "SBU1", "GOLD", "FDI",
            "0301478523", "0283821800", "vietnam@keppel.com", "Saigon Centre Tower 2, Q.1, TP.HCM",
            "Ông Joseph Tan / Nguyễn Văn Đức", "Giám Đốc Quản Lý Xây Dựng", "0903445566", "1980-05-12", "1992-06-15",
            5, "EXCELLENT", "Yêu cầu kỹ thuật ngầm tiêu chuẩn Singapore, độ an toàn tuyệt đối. Đạt chuẩn Hạng Vàng.",
            15.0, 12.5, 25.0, 10.0, 7.5, 70.0, 0, 0, 20000000.0, 4500000.0,
            "TGĐ / SBU Leader phụ trách", "3 tháng / lần"
        ),
        (
            "KH-SBU1-03", "Công ty Cổ phần Đầu tư Xây dựng Nam Hải", "SBU1", "SILVER", "B2B",
            "0317889911", "0283711223", "info@namhai-cons.vn", "Khu đô thị Sala, TP. Thủ Đức, TP.HCM",
            "Ông Trần Nam Hải", "Chủ Tịch HĐQT", "0918776655", "1982-03-15", "2018-09-10",
            4, "STABLE", "Đối tác tiềm năng các gói thầu xử lý nền đất yếu và cọc xi măng đất đường nội bộ.",
            7.5, 12.5, 12.5, 0.0, 7.5, 40.0, 0, 0, 5000000.0, 800000.0,
            "SBU Leader / GĐKD phụ trách", "Theo sự vụ thực tế"
        ),

        # SBU 2 - Năng lượng và công nghiệp
        (
            "KH-SBU2-01", "Tổng Công ty Cổ phần Dịch vụ Kỹ thuật Dầu khí Việt Nam (PTSC)", "SBU2", "DIAMOND", "B2B",
            "0100150577", "0283910282", "ptsc@ptsc.com.vn", "Lầu 5, Tòa nhà PetroVietnam, 1-5 Lê Duẩn, Q.1, TP.HCM",
            "Ông Lê Mạnh Cường", "Tổng Giám Đốc", "0918889900", "1976-09-02", "1993-02-18",
            5, "EXCELLENT", "Đầu mối quan hệ chiến lược cho các gói thầu xây lắp chân đế điện gió ngoài khơi và kho cảng LNG.",
            15.0, 25.0, 25.0, 20.0, 15.0, 100.0, 0, 0, 80000000.0, 12000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),
        (
            "KH-SBU2-02", "Tập đoàn Bán dẫn Amkor Technology Việt Nam", "SBU2", "DIAMOND", "FDI",
            "2301198888", "0222388999", "amkor_vn@amkor.com", "KCN Yên Phong II-C, Yên Phong, Bắc Ninh",
            "Ông Kim Sung-Hoon / Trần Đức Tuấn", "Phó TGĐ Điều Hành Xây Lắp", "0982334455", "1979-11-05", "2021-10-10",
            5, "EXCELLENT", "Khách hàng FDI trọng điểm. Yêu cầu sàn phòng sạch không rung chấn Micro-vibration.",
            15.0, 25.0, 25.0, 10.0, 15.0, 90.0, 0, 0, 80000000.0, 8000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),

        # SBU 3 - Metro và ngầm đô thị
        (
            "KH-SBU3-01", "Ban Quản lý Đường sắt Đô thị TP.HCM (MAUR)", "SBU3", "DIAMOND", "B2G",
            "0313112233", "0283822456", "maur@tphcm.gov.vn", "29 Lê Quý Đôn, P. Võ Thị Sáu, Q.3, TP.HCM",
            "Ông Bùi Anh Tuấn", "Trưởng Ban Quản Lý Dự Án Metro 2", "0913998811", "1974-12-25", "2007-09-13",
            5, "EXCELLENT", "Ban QLDA Nhà nước trọng điểm. Cơ hội lớn cho gói thầu TBM khoan hầm và ga ngầm Bến Thành.",
            15.0, 25.0, 25.0, 20.0, 10.0, 95.0, 0, 0, 80000000.0, 18000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),
        (
            "KH-SBU3-02", "Ban Quản lý Đường sắt Đô thị Hà Nội (MRB)", "SBU3", "GOLD", "B2G",
            "0104112244", "0243773898", "mrb.hanoi@hanoi.gov.vn", "Tòa nhà Cung Trí Thức, Cầu Giấy, Hà Nội",
            "Ông Nguyễn Cao Minh", "Phó Trưởng Ban Thường Trực", "0912776655", "1975-08-19", "2008-07-28",
            4, "STABLE", "Đang theo sát hồ sơ thiết kế kỹ thuật đoạn ngầm tuyến Metro số 3 và tuyến số 5.",
            7.5, 25.0, 12.5, 10.0, 7.5, 62.5, 0, 0, 20000000.0, 3000000.0,
            "TGĐ / SBU Leader phụ trách", "3 tháng / lần"
        ),

        # SBU 4 - Hạ tầng tập trung và đường sắt cao tốc
        (
            "KH-SBU4-01", "Tổng Công ty Cảng Hàng không Việt Nam (ACV)", "SBU4", "DIAMOND", "B2G",
            "0311638525", "0283844335", "info@vietnamairport.vn", "58 Trường Sơn, P.2, Tân Bình, TP.HCM",
            "Ông Lại Xuân Thanh", "Chủ Tịch Hội Đồng Quản Trị", "0903778899", "1963-04-10", "2012-02-08",
            5, "EXCELLENT", "Chủ đầu tư Siêu dự án Cảng HKQT Long Thành và Nhà ga T3 Tân Sơn Nhất.",
            15.0, 25.0, 25.0, 20.0, 15.0, 100.0, 0, 0, 80000000.0, 22000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),
        (
            "KH-SBU4-02", "Ban Quản lý Dự án Đường sắt (Bộ Giao thông Vận tải)", "SBU4", "DIAMOND", "B2G",
            "0100109988", "0243942388", "pmu-railway@mt.gov.vn", "Số 118 Lê Duẩn, Hoàn Kiếm, Hà Nội",
            "Ông Vũ Hồng Phương", "Giám Đốc Ban Quản Lý", "0913554433", "1971-06-28", "1998-05-15",
            5, "EXCELLENT", "Cơ quan đầu mối chuẩn bị đầu tư Đại dự án Đường sắt tốc độ cao Bắc - Nam 70 tỷ USD.",
            15.0, 25.0, 25.0, 10.0, 7.5, 82.5, 0, 0, 80000000.0, 5000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),

        # SBU 5 - Cảng biển và biến đổi khí hậu
        (
            "KH-SBU5-01", "Tổng Công ty Tân Cảng Sài Gòn (Saigon Newport Corporation)", "SBU5", "DIAMOND", "B2B",
            "0300445566", "0283742223", "marketing@saigonnewport.com.vn", "722 Điện Biên Phủ, P.22, Bình Thạnh, TP.HCM",
            "Đại tá Ngô Minh Thuấn", "Tổng Giám Đốc", "0903991122", "1970-10-15", "1989-03-15",
            5, "EXCELLENT", "Tổng công ty khai thác cảng biển số 1 VN, đang triển khai chuỗi cảng nước sâu Cái Mép và Cần Giờ.",
            15.0, 25.0, 25.0, 20.0, 15.0, 100.0, 0, 0, 80000000.0, 16000000.0,
            "Chủ tịch / TGĐ trực tiếp phụ trách", "1 tháng / lần"
        ),
        (
            "KH-SBU5-02", "Ban QLDA Đầu tư Xây dựng Hạ tầng Đô thị TP.HCM", "SBU5", "GOLD", "B2G",
            "0315223344", "0283930123", "bql.hatang@tphcm.gov.vn", "Số 3 Bà Huyện Thanh Quan, Q.3, TP.HCM",
            "Ông Bùi Ngọc Đức", "Phó Giám Đốc Điều Hành", "0918667788", "1977-03-22", "2019-02-15",
            4, "STABLE", "Chủ đầu tư các dự án cải tạo kênh rạch, đê kè chống sạt lở và thích ứng biến đổi khí hậu.",
            7.5, 25.0, 12.5, 10.0, 7.5, 62.5, 0, 0, 20000000.0, 2000000.0,
            "TGĐ / SBU Leader phụ trách", "3 tháng / lần"
        ),
        (
            "KH-SBU5-03", "Công ty Cổ phần Đầu tư Phát triển Biển Tây", "SBU5", "GOLD", "B2B",
            "0319988776", "0297388990", "contact@bientay-invest.vn", "Khu lấn biển Trần Phú, TP. Rạch Giá, Kiên Giang",
            "Ông Vương Đình Toàn", "Tổng Giám Đốc", "0909554433", "1984-06-18", "2020-05-12",
            3, "NEEDS_ATTENTION", "Chủ đầu tư dự án khu nghỉ dưỡng lấn biển. Áp dụng quy tắc phủ quyết do dòng tiền rủi ro.",
            15.0, 25.0, 0.0, 10.0, 7.5, 57.5, 0, 1, 20000000.0, 1500000.0,
            "TGĐ / SBU Leader phụ trách", "3 tháng / lần"
        )
    ]

    cursor.executemany("""
    INSERT INTO customers (
        code, name, sbu, tier, segment, tax_code, phone, email, headquarters,
        key_decision_maker, decision_maker_role, decision_maker_phone,
        decision_maker_birthday, founding_anniversary, relationship_score,
        relationship_status, strategic_notes,
        score_scale_project, score_fecon_fit, score_financial_capacity,
        score_cooperation_history, score_management_capacity, total_score,
        is_special_elevated, veto_applied, annual_care_budget, spent_care_budget,
        in_charge_executive, care_frequency
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, customers_data)

    # 3. PIPELINE BIDS & OPPORTUNITIES (Theo 5 SBU)
    bids_data = [
        # SBU 1
        (1, "SBU1", "Thi công Cọc Barrette D1200 & 5 Tầng hầm Khối Tháp Đôi Grand Marina", 480000000000, "WON", 100, (today - timedelta(days=90)).strftime(d_fmt), "KS. Đỗ Hoàng Long", "Đã ký hợp đồng tổng thầu thi công ngầm."),
        (2, "SBU1", "Thi công Cọc Khoan Nhồi D1500 & Móng Sâu Khu Phức Hợp Saigon Sports City", 350000000000, "TENDER_PREP", 75, (today + timedelta(days=20)).strftime(d_fmt), "KS. Đỗ Hoàng Long", "Đang hoàn thiện giải pháp biện pháp thi công khoan thổi phôi."),
        
        # SBU 2
        (3, "SBU2", "Tổng thầu Xây lắp Cảng chuyên dùng & Bồn chứa LNG Giai đoạn 2", 820000000000, "WON", 100, (today - timedelta(days=120)).strftime(d_fmt), "ThS. Nguyễn Quốc Thái", "Trúng thầu nhờ kinh nghiệm EPC công nghiệp dầu khí."),
        (4, "SBU2", "Gói thầu Kết cấu Nhà xưởng Công nghệ cao & Phòng sạch Amkor Phase 2", 560000000000, "NEGOTIATION", 85, (today + timedelta(days=15)).strftime(d_fmt), "ThS. Nguyễn Quốc Thái", "Đang chốt điều khoản bảo lãnh tín dụng ngân hàng."),

        # SBU 3
        (5, "SBU3", "Tuyến Metro số 2 TP.HCM - Gói thầu CP2: Ga Ngầm Trung Tâm & Đoạn Hầm TBM", 1450000000000, "EVALUATION", 60, (today + timedelta(days=45)).strftime(d_fmt), "KS. Vũ Trọng Khôi", "Liên danh với đối tác Nhật Bản tham gia sơ tuyển kỹ thuật."),
        (6, "SBU3", "Gói thầu Hầm Ngầm Dẫn S12 - Ga Hà Nội Tuyến Metro 3", 620000000000, "INFORMATION", 40, (today + timedelta(days=60)).strftime(d_fmt), "KS. Vũ Trọng Khôi", "Tiếp cận hồ sơ mời sơ tuyển quốc tế."),

        # SBU 4
        (7, "SBU4", "Dự án Cảng HKQT Long Thành - Gói thầu 4.6: Hệ thống Đường cất hạ cánh & Sân đỗ", 2650000000000, "WON", 100, (today - timedelta(days=150)).strftime(d_fmt), "ThS. Lê Thành Trung", "Gói thầu hạ tầng hàng không trọng điểm quốc gia."),
        (8, "SBU4", "Đại dự án Đường sắt Tốc độ cao Bắc - Nam: Gói tư vấn khảo sát & cọc thí nghiệm", 890000000000, "INFORMATION", 30, (today + timedelta(days=90)).strftime(d_fmt), "ThS. Lê Thành Trung", "Lãnh đạo Bộ GTVT đánh giá cao năng lực thi công hạ tầng đường sắt."),

        # SBU 5
        (9, "SBU5", "Thi công Bến số 7, 8 Cảng Cửa ngõ Quốc tế Lạch Huyện (Hải Phòng)", 1120000000000, "WON", 100, (today - timedelta(days=180)).strftime(d_fmt), "KS. Trần Đình Bách", "Gói thầu cọc ống thép SPP D1200 đóng ngoài biển."),
        (10, "SBU5", "Dự án Kè biển Bảo vệ Bờ và Cống Kiểm soát Triều Thích ứng BĐKH Cần Giờ", 430000000000, "TENDER_PREP", 70, (today + timedelta(days=25)).strftime(d_fmt), "KS. Trần Đình Bách", "Đang tối ưu giải pháp khối bê tông Haro giảm sóng.")
    ]

    cursor.executemany("""
    INSERT INTO pipeline_bids (
        customer_id, sbu, project_title, estimated_value, stage, win_rate,
        tender_deadline, assigned_director, bidding_notes
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, bids_data)

    # 4. EXECUTIVE PROJECTS (Theo dõi vĩ mô 5 SBU)
    projects_data = [
        # SBU 1
        (
            "DA-SBU1-01", "Tổ hợp Móng & Tầng Hầm Grand Marina Saigon (5 Hầm)", "SBU1", 1,
            "HD-MM/2025-GM01", 480000000000, 312000000000, 65.0,
            (today - timedelta(days=180)).strftime(d_fmt), (today + timedelta(days=120)).strftime(d_fmt),
            "GOOD", "KS. Đỗ Hoàng Long", "Tường vây dày 1.5m, sâu 48m, 5 tầng hầm sâu 22m giữa trung tâm Q.1."
        ),
        # SBU 2
        (
            "DA-SBU2-01", "Hạ tầng Xây Lắp Kho Cảng & Bồn Chứa Khí Hóa Lỏng LNG Thị Vải", "SBU2", 3,
            "HD-PTSC/LNG-2025", 820000000000, 574000000000, 70.0,
            (today - timedelta(days=220)).strftime(d_fmt), (today + timedelta(days=90)).strftime(d_fmt),
            "GOOD", "ThS. Nguyễn Quốc Thái", "Bồn chứa dung tích 180.000 m3, đường ống công nghệ chịu áp lực và lạnh sâu."
        ),
        # SBU 3
        (
            "DA-SBU3-01", "Ga Ngầm Nhà Thờ Đức Bà & Đoạn Hầm Metro Tuyến 2 (Giai đoạn kỹ thuật)", "SBU3", 5,
            "HD-MAUR/M2-CP02", 350000000000, 105000000000, 30.0,
            (today - timedelta(days=90)).strftime(d_fmt), (today + timedelta(days=360)).strftime(d_fmt),
            "ATTENTION", "KS. Vũ Trọng Khôi", "Đang xử lý di dời công trình hạ tầng kỹ thuật ngầm và quan trắc địa kỹ thuật."
        ),
        # SBU 4
        (
            "DA-SBU4-01", "Cảng Hàng Không Quốc Tế Long Thành - Gói Thầu 4.6 (Đường băng & Sân đỗ)", "SBU4", 7,
            "HD-ACV/LT-4.6", 2650000000000, 1590000000000, 60.0,
            (today - timedelta(days=150)).strftime(d_fmt), (today + timedelta(days=210)).strftime(d_fmt),
            "GOOD", "ThS. Lê Thành Trung", "Đường cất hạ cánh số 1 dài 4.000m, rộng 75m, hệ thống đường lăn và sân đỗ 85 vị trí."
        ),
        # SBU 5
        (
            "DA-SBU5-01", "Bến Số 7 & 8 Cảng Cửa Ngõ Quốc Tế Hải Phòng (Lạch Huyện)", "SBU5", 9,
            "HD-SNP/LH-B78", 1120000000000, 784000000000, 70.0,
            (today - timedelta(days=200)).strftime(d_fmt), (today + timedelta(days=100)).strftime(d_fmt),
            "GOOD", "KS. Trần Đình Bách", "Chiều dài 2 bến 900m tiếp nhận tàu container trọng tải đến 18.000 TEU."
        )
    ]

    cursor.executemany("""
    INSERT INTO projects (
        code, name, sbu, customer_id, contract_number, contract_value, paid_amount,
        progress_percent, start_date, expected_end_date, project_health, project_director, summary_scope
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, projects_data)

    # 5. MILESTONES (Dòng tiền giải ngân cấp lãnh đạo)
    milestones_data = [
        # Dự án 1: Grand Marina (SBU1)
        (1, "Nghiệm thu hoàn thành hệ tường vây và đào đất hầm B3", (today - timedelta(days=20)).strftime(d_fmt), 20.0, 96000000000, "PAID", "Đã thanh toán đúng hạn"),
        (1, "Nghiệm thu đổ bê tông sàn nắp hầm B1 và đóng nắp hầm", (today + timedelta(days=25)).strftime(d_fmt), 25.0, 120000000000, "INVOICED", "Đang chuẩn bị hồ sơ nghiệm thu A-B"),
        (1, "Quyết toán hoàn thành phần ngầm & Bàn giao mặt bằng kết cấu thân", (today + timedelta(days=110)).strftime(d_fmt), 15.0, 72000000000, "PENDING", "Mốc kết thúc hợp đồng"),

        # Dự án 2: LNG Thị Vải (SBU2)
        (2, "Nghiệm thu lắp đặt vỏ bồn Niken và thử áp lực thủy tĩnh", (today - timedelta(days=15)).strftime(d_fmt), 30.0, 246000000000, "PAID", "Đạt chuẩn an toàn quốc tế"),
        (2, "Nghiệm thu đấu nối đường ống công nghệ và đóng điện lạnh sâu", (today + timedelta(days=35)).strftime(d_fmt), 25.0, 205000000000, "INVOICED", "Chờ giải ngân đợt 4"),

        # Dự án 4: Long Thành (SBU4)
        (4, "Nghiệm thu thảm bê tông xi măng lớp mặt đường cất hạ cánh đợt 2", (today - timedelta(days=10)).strftime(d_fmt), 20.0, 530000000000, "PAID", "ACV giải ngân đúng tiến độ"),
        (4, "Nghiệm thu hệ thống đèn tín hiệu đường băng và đường lăn kết nối", (today + timedelta(days=40)).strftime(d_fmt), 20.0, 530000000000, "INVOICED", "Đang kiểm định bay hiệu chuẩn"),

        # Dự án 5: Lạch Huyện (SBU5)
        (5, "Nghiệm thu đóng 100% cọc ống thép SPP và bản mặt cầu cảng bến 7", (today - timedelta(days=30)).strftime(d_fmt), 35.0, 392000000000, "PAID", "Đạt nghiệm thu cảng biển"),
        (5, "Lắp đặt hệ thống đệm va và bích neo tàu 150.000 tấn", (today + timedelta(days=50)).strftime(d_fmt), 25.0, 280000000000, "PENDING", "Chờ hàng nhập khẩu cập cảng")
    ]

    cursor.executemany("""
    INSERT INTO milestones (project_id, title, due_date, percentage, amount, payment_status, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?);
    """, milestones_data)

    # 6. EXECUTIVE CARE & NETWORKING ACTIVITIES (Chăm sóc & Ngoại giao cấp cao)
    activities_data = [
        (
            1, "SBU1", "DINNER_NETWORKING", "Bữa tối thân mật giữa Chủ tịch HĐQT và Ban Lãnh Đạo Masterise Homes",
            "Chủ tịch HĐQT và GĐKD SBU1 tiếp đón Phó TGĐ Masterise. Hai bên thống nhất nguyên tắc hợp tác dài hạn cho chuỗi 3 dự án căn hộ cao cấp ven sông chuẩn bị khởi công năm 2027.",
            (today - timedelta(days=5)).strftime(d_fmt), "Chủ Tịch HĐQT & KS. Đỗ Hoàng Long", "SUCCESS", 15000000.0
        ),
        (
            4, "SBU2", "EXECUTIVE_MEETING", "Họp chiến lược hợp tác phát triển điện gió ngoài khơi với TGĐ PTSC",
            "Đoàn lãnh đạo công ty làm việc tại trụ sở PTSC. Thảo luận về việc chuẩn bị năng lực bãi chế tạo và phương án thi công móng trụ điện gió xuất khẩu sang Singapore.",
            (today - timedelta(days=8)).strftime(d_fmt), "Tổng Giám Đốc & ThS. Nguyễn Quốc Thái", "SUCCESS", 12000000.0
        ),
        (
            6, "SBU3", "EXECUTIVE_MEETING", "Làm việc định kỳ với Lãnh đạo Ban Quản lý Đường sắt Đô thị TP.HCM",
            "Báo cáo giải pháp xử lý địa chất ngầm phức tạp ga Bến Thành. Ban QLDA đánh giá rất cao tinh thần trách nhiệm và cam kết kỹ thuật của nhà thầu.",
            (today - timedelta(days=12)).strftime(d_fmt), "Chủ Tịch HĐQT & KS. Vũ Trọng Khôi", "SUCCESS", 5000000.0
        ),
        (
            8, "SBU4", "GIFT_DELIVERY", "Thăm và chúc mừng Chủ tịch HĐQT Tổng Công ty Cảng Hàng Không ACV",
            "Ban Lãnh Đạo gửi quà tri ân nhân dịp đạt mốc vượt tiến độ 60 ngày tại đại dự án Cảng HKQT Long Thành.",
            (today - timedelta(days=18)).strftime(d_fmt), "Chủ Tịch HĐQT & ThS. Lê Thành Trung", "SUCCESS", 10000000.0
        ),
        (
            10, "SBU5", "EVENT_INVITATION", "Tham dự Lễ Kỷ niệm 35 Năm Ngày Truyền thống Tân Cảng Sài Gòn",
            "Đoàn đại biểu công ty do TGĐ dẫn đầu tham dự lễ kỷ niệm và chúc mừng Đại tá Ngô Minh Thuấn - Tổng Giám Đốc Tân Cảng.",
            (today - timedelta(days=22)).strftime(d_fmt), "Tổng Giám Đốc & KS. Trần Đình Bách", "SUCCESS", 6000000.0
        )
    ]

    cursor.executemany("""
    INSERT INTO customer_care_activities (customer_id, sbu, activity_type, title, content, occurred_at, leader_in_charge, outcome_status, cost)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, activities_data)

    # 7. AUTOMATED EXECUTIVE MESSAGES
    messages_data = [
        (
            1, 1, "SBU1", "ZALO_ZNS", "CHUC_MUNG_SINH_NHAT", "0908112233",
            "[Zalo ZNS Lãnh Đạo] Chúc mừng Sinh nhật Phó TGĐ Phan Trọng Đạt",
            "Kính gửi Ông Phan Trọng Đạt (Phó TGĐ Masterise Homes): Nhân dịp sinh nhật của Anh, Ban Lãnh Đạo Công ty Xây dựng xin kính chúc Anh tuổi mới ngập tràn niềm vui, dồi dào sức khỏe, lãnh đạo tập đoàn ngày càng vươn xa và tiếp tục đồng hành bền chặt cùng chúng tôi!",
            "SENT", (today - timedelta(days=2)).strftime("%Y-%m-%d %H:%M:%S")
        ),
        (
            7, 4, "SBU4", "SMS", "THONG_BAO_NGHIEM_THU", "0903778899",
            "[SMS Brandname] Báo cáo mốc nghiệm thu thảm mặt đường cất hạ cánh Long Thành",
            "Kính gửi Chủ tịch ACV Lại Xuân Thanh: Hạng mục bê tông xi măng lớp mặt đường băng số 1 Long Thành đã nghiệm thu đạt 100% cường độ R28. Giá trị đề nghị giải ngân mốc 530 Tỷ VNĐ. Trân trọng báo cáo!",
            "SENT", (today - timedelta(days=10)).strftime("%Y-%m-%d %H:%M:%S")
        ),
        (
            9, 5, "SBU5", "ZALO_ZNS", "THANH_LAP_DOI_TAC", "0903991122",
            "[Zalo ZNS] Chúc mừng Ngày truyền thống Tân Cảng Sài Gòn",
            "Ban Lãnh Đạo Công ty Xây dựng trân trọng chúc mừng Tổng công ty Tân Cảng Sài Gòn và Đại tá Ngô Minh Thuấn nhân dịp kỷ niệm ngày truyền thống. Kính chúc Tân Cảng luôn giữ vững vị thế số 1 ngành khai thác cảng và logistics biển!",
            "SENT", (today - timedelta(days=22)).strftime("%Y-%m-%d %H:%M:%S")
        )
    ]

    cursor.executemany("""
    INSERT INTO automated_messages (customer_id, project_id, sbu, channel, template_type, recipient, title, message_body, status, sent_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, messages_data)

    conn.commit()
    conn.close()
    print("New Executive CRM seed data for 5 SBUs successfully planted!")

if __name__ == "__main__":
    seed_database()
