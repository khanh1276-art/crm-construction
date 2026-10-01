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
from app.database import get_db, init_db, classify_fecon_project

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

    # 3. PIPELINE BIDS & OPPORTUNITIES (Theo 5 SBU - Đủ 5 Cấp Dự Án FECON)
    raw_bids_data = [
        # SBU 1
        (1, "SBU1", "Thi công Cọc Barrette D1200 & 5 Tầng hầm Khối Tháp Đôi Grand Marina", 480000000000, "WON", 100, (today - timedelta(days=90)).strftime(d_fmt), "KS. Đỗ Hoàng Long", "Đã ký hợp đồng tổng thầu thi công ngầm."),
        (2, "SBU1", "Thi công Cọc Khoan Nhồi D1500 Khu Phức Hợp Saigon Sports City", 185000000000, "TENDER_PREP", 75, (today + timedelta(days=20)).strftime(d_fmt), "KS. Đỗ Hoàng Long", "Đang hoàn thiện giải pháp biện pháp thi công khoan thổi phôi."),
        (3, "SBU1", "Gói thầu Cọc Xi Măng Đất CDM Đường Giao Thông Nội Bộ Sala", 42000000000, "NEGOTIATION", 90, (today + timedelta(days=10)).strftime(d_fmt), "KS. Đỗ Hoàng Long", "Thương thảo điều khoản tạm ứng 20%."),
        
        # SBU 2
        (4, "SBU2", "Tổng thầu Xây lắp Cảng chuyên dùng & Bồn chứa LNG Giai đoạn 2", 820000000000, "WON", 100, (today - timedelta(days=120)).strftime(d_fmt), "ThS. Nguyễn Quốc Thái", "Trúng thầu nhờ kinh nghiệm EPC công nghiệp dầu khí."),
        (5, "SBU2", "Gói thầu Kết cấu Nhà xưởng Công nghệ cao & Phòng sạch Amkor Phase 2", 380000000000, "NEGOTIATION", 85, (today + timedelta(days=15)).strftime(d_fmt), "ThS. Nguyễn Quốc Thái", "Đang chốt điều khoản bảo lãnh tín dụng ngân hàng."),
        (4, "SBU2", "Khảo sát Địa kỹ thuật Ngoài khơi & Thí nghiệm Cọc Điện gió La Gàn", 28000000000, "WON", 100, (today - timedelta(days=60)).strftime(d_fmt), "ThS. Nguyễn Quốc Thái", "Đã huy động tàu khoan khảo sát biển."),

        # SBU 3
        (6, "SBU3", "Tuyến Metro số 2 TP.HCM - Gói thầu CP2: Ga Ngầm Trung Tâm & Đoạn Hầm TBM", 1450000000000, "EVALUATION", 60, (today + timedelta(days=45)).strftime(d_fmt), "KS. Vũ Trọng Khôi", "Liên danh với đối tác Nhật Bản tham gia sơ tuyển kỹ thuật."),
        (7, "SBU3", "Gói thầu Hầm Ngầm Dẫn S12 - Ga Hà Nội Tuyến Metro 3", 95000000000, "INFORMATION", 40, (today + timedelta(days=60)).strftime(d_fmt), "KS. Vũ Trọng Khôi", "Tiếp cận hồ sơ mời sơ tuyển quốc tế."),

        # SBU 4
        (8, "SBU4", "Dự án Cảng HKQT Long Thành - Gói thầu 4.6: Hệ thống Đường cất hạ cánh & Sân đỗ", 2650000000000, "WON", 100, (today - timedelta(days=150)).strftime(d_fmt), "ThS. Lê Thành Trung", "Gói thầu hạ tầng hàng không trọng điểm quốc gia."),
        (9, "SBU4", "Tuyến Đường Vành Đai 4 - Gói Gia Cố Nền Đất Yếu & Cọc CDM", 125000000000, "TENDER_PREP", 80, (today + timedelta(days=30)).strftime(d_fmt), "ThS. Lê Thành Trung", "Bộ GTVT đánh giá cao giải pháp rút ngắn tiến độ."),

        # SBU 5
        (10, "SBU5", "Thi công Bến số 7, 8 Cảng Cửa ngõ Quốc tế Lạch Huyện (Hải Phòng)", 1120000000000, "WON", 100, (today - timedelta(days=180)).strftime(d_fmt), "KS. Trần Đình Bách", "Gói thầu cọc ống thép SPP D1200 đóng ngoài biển."),
        (10, "SBU5", "Bến Cảng Container Cái Mép Gemalink Giai Đoạn 2", 220000000000, "TENDER_PREP", 70, (today + timedelta(days=25)).strftime(d_fmt), "KS. Trần Đình Bách", "Đang tối ưu giải pháp bến nhô tiếp nhận tàu 200.000 DWT."),
        (11, "SBU5", "Kè Chống Sạt Lở Bờ Kênh Đôi & Cống Kiểm Soát Triều Thích Ứng BĐKH", 38000000000, "WON", 100, (today - timedelta(days=40)).strftime(d_fmt), "KS. Trần Đình Bách", "Dự án vốn ngân sách TP.HCM đã hoàn tất bảo lãnh.")
    ]

    bids_data = []
    for row in raw_bids_data:
        cid, sbu, title, eval_val, stage, win_rate, deadline, director, notes = row
        cls_info = classify_fecon_project(eval_val)
        bids_data.append((
            cid, sbu, title, eval_val, stage, win_rate, deadline, director, notes,
            cls_info["level"], cls_info["level_name"], cls_info["approver_authority"]
        ))

    cursor.executemany("""
    INSERT INTO pipeline_bids (
        customer_id, sbu, project_title, estimated_value, stage, win_rate,
        tender_deadline, assigned_director, bidding_notes,
        project_level, project_level_name, approver_authority
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, bids_data)

    # 4. EXECUTIVE PROJECTS (Theo dõi vĩ mô 5 SBU - Đủ Cả 5 Cấp Dự Án FECON)
    raw_projects = [
        # --- CẤP ĐẶC BIỆT (≥ 500 TỶ) - Phê duyệt: Chủ tịch HĐQT ---
        (
            "DA-SBU4-01", "Cảng Hàng Không Quốc Tế Long Thành - Gói Thầu 4.6 (Đường băng & Sân đỗ)", "SBU4", 8,
            "HD-ACV/LT-4.6", 2650000000000, 1590000000000, 60.0,
            (today - timedelta(days=150)).strftime(d_fmt), (today + timedelta(days=210)).strftime(d_fmt),
            "GOOD", "ThS. Lê Thành Trung", "Đường cất hạ cánh số 1 dài 4.000m, rộng 75m, hệ thống đường lăn và sân đỗ 85 vị trí."
        ),
        (
            "DA-SBU5-01", "Bến Số 7 & 8 Cảng Cửa Ngõ Quốc Tế Hải Phòng (Lạch Huyện)", "SBU5", 10,
            "HD-SNP/LH-B78", 1120000000000, 784000000000, 70.0,
            (today - timedelta(days=200)).strftime(d_fmt), (today + timedelta(days=100)).strftime(d_fmt),
            "GOOD", "KS. Trần Đình Bách", "Chiều dài 2 bến 900m tiếp nhận tàu container trọng tải đến 18.000 TEU."
        ),
        (
            "DA-SBU2-01", "Hạ tầng Xây Lắp Kho Cảng & Bồn Chứa Khí Hóa Lỏng LNG Thị Vải", "SBU2", 4,
            "HD-PTSC/LNG-2025", 820000000000, 574000000000, 70.0,
            (today - timedelta(days=220)).strftime(d_fmt), (today + timedelta(days=90)).strftime(d_fmt),
            "GOOD", "ThS. Nguyễn Quốc Thái", "Bồn chứa dung tích 180.000 m3, đường ống công nghệ chịu áp lực và lạnh sâu."
        ),

        # --- CẤP 1 (300 - < 500 TỶ) - Phê duyệt: Tổng Giám Đốc (hoặc PTGĐ ủy quyền) ---
        (
            "DA-SBU1-01", "Tổ hợp Móng & Tầng Hầm Grand Marina Saigon (5 Hầm)", "SBU1", 1,
            "HD-MM/2025-GM01", 480000000000, 312000000000, 65.0,
            (today - timedelta(days=180)).strftime(d_fmt), (today + timedelta(days=120)).strftime(d_fmt),
            "GOOD", "KS. Đỗ Hoàng Long", "Tường vây dày 1.5m, sâu 48m, 5 tầng hầm sâu 22m giữa trung tâm Q.1."
        ),
        (
            "DA-SBU2-02", "Nhà Xưởng Công Nghệ Cao & Phòng Sạch Amkor Phase 2", "SBU2", 5,
            "HD-AMK/P2-2025", 380000000000, 190000000000, 50.0,
            (today - timedelta(days=100)).strftime(d_fmt), (today + timedelta(days=180)).strftime(d_fmt),
            "GOOD", "ThS. Nguyễn Quốc Thái", "Yêu cầu kết cấu tiêu chuẩn phòng sạch Class 1000 và triệt tiêu rung chấn micro-vibration."
        ),
        (
            "DA-SBU3-01", "Ga Ngầm Nhà Thờ Đức Bà & Đoạn Hầm Metro Tuyến 2 (Giai đoạn kỹ thuật)", "SBU3", 6,
            "HD-MAUR/M2-CP02", 350000000000, 105000000000, 30.0,
            (today - timedelta(days=90)).strftime(d_fmt), (today + timedelta(days=360)).strftime(d_fmt),
            "ATTENTION", "KS. Vũ Trọng Khôi", "Đang xử lý di dời công trình hạ tầng kỹ thuật ngầm và quan trắc địa kỹ thuật."
        ),

        # --- CẤP 2 (150 - < 300 TỶ) - Phê duyệt: Phó Tổng Giám Đốc phụ trách SBU ---
        (
            "DA-SBU5-02", "Bến Cảng Container Quốc Tế Gemalink Cái Mép - Giai Đoạn 2", "SBU5", 10,
            "HD-SNP/GEM2-2025", 220000000000, 110000000000, 50.0,
            (today - timedelta(days=80)).strftime(d_fmt), (today + timedelta(days=160)).strftime(d_fmt),
            "GOOD", "KS. Trần Đình Bách", "Gói thầu thi công cầu dẫn và trụ va neo bến cảng nước sâu 200.000 tấn."
        ),
        (
            "DA-SBU1-02", "Khu Phức Hợp Saigon Sports City - Móng Sâu Cọc Khoan Nhồi D1500", "SBU1", 2,
            "HD-KL/SSC-02", 185000000000, 92500000000, 50.0,
            (today - timedelta(days=70)).strftime(d_fmt), (today + timedelta(days=140)).strftime(d_fmt),
            "GOOD", "KS. Đỗ Hoàng Long", "Khoan nhồi đường kính 1.5m, chiều sâu mũi cọc 72m cắm vào tầng cuội sỏi."
        ),

        # --- CẤP 3 (50 - < 150 TỶ) - Phê duyệt: Phó Tổng Giám Đốc phụ trách SBU ---
        (
            "DA-SBU4-02", "Tuyến Đường Vành Đai 4 - Gói Xử Lý Nền Đất Yếu & Cọc CDM", "SBU4", 9,
            "HD-PMU/VD4-XL03", 125000000000, 62500000000, 50.0,
            (today - timedelta(days=60)).strftime(d_fmt), (today + timedelta(days=120)).strftime(d_fmt),
            "GOOD", "ThS. Lê Thành Trung", "Gia cố 18km nền đường cao tốc qua vùng đất yếu bằng cọc xi măng đất và bấc thấm."
        ),
        (
            "DA-SBU3-02", "Đoạn Hầm Dẫn S12 Ga Hà Nội - Tuyến Metro 3", "SBU3", 7,
            "HD-MRB/M3-S12", 95000000000, 38000000000, 40.0,
            (today - timedelta(days=50)).strftime(d_fmt), (today + timedelta(days=150)).strftime(d_fmt),
            "GOOD", "KS. Vũ Trọng Khôi", "Gia cố vòm ngầm chống sụt lún công trình lịch sử khu vực trung tâm ga Hà Nội."
        ),

        # --- CẤP 4 (< 50 TỶ) - Phê duyệt: Phó Tổng Giám Đốc phụ trách SBU ---
        (
            "DA-SBU5-03", "Kè Chống Sạt Lở & Cống Kiểm Soát Triều Thích Ứng BĐKH Kênh Đôi", "SBU5", 11,
            "HD-HTDT/KD-05", 38000000000, 19000000000, 50.0,
            (today - timedelta(days=45)).strftime(d_fmt), (today + timedelta(days=90)).strftime(d_fmt),
            "GOOD", "KS. Trần Đình Bách", "Kè bảo vệ bờ kết hợp cừ dự ứng lực SW và nạo vét khơi thông dòng chảy."
        ),
        (
            "DA-SBU2-03", "Khảo Sát Địa Kỹ Thuật & Thí Nghiệm Cọc Điện Gió Ngoài Khơi La Gàn", "SBU2", 4,
            "HD-LG/GEO-2025", 28000000000, 28000000000, 100.0,
            (today - timedelta(days=110)).strftime(d_fmt), (today - timedelta(days=10)).strftime(d_fmt),
            "GOOD", "ThS. Nguyễn Quốc Thái", "Khảo sát địa chấn nông và khoan lấy mẫu tầng móng ngoài khơi vùng biển Bình Thuận."
        ),
        (
            "DA-SBU1-03", "Thi Công Cọc Xi Măng Đất CDM Tuyến Đường Trục Khu Đô Thị Sala", "SBU1", 3,
            "HD-NH/SALA-2025", 42000000000, 29400000000, 70.0,
            (today - timedelta(days=75)).strftime(d_fmt), (today + timedelta(days=45)).strftime(d_fmt),
            "GOOD", "KS. Đỗ Hoàng Long", "Xử lý nền đường giao thông bằng công nghệ Jet Grouting cọc D800."
        )
    ]

    projects_data = []
    for p in raw_projects:
        code, name, sbu, cust_id, contract_no, val, paid, prog, s_date, e_date, health, director, scope = p
        cls_info = classify_fecon_project(val)
        projects_data.append((
            code, name, sbu, cust_id, contract_no, val, paid, prog, s_date, e_date, health, director, scope,
            cls_info["level"], cls_info["level_name"], cls_info["approver_authority"]
        ))

    cursor.executemany("""
    INSERT INTO projects (
        code, name, sbu, customer_id, contract_number, contract_value, paid_amount,
        progress_percent, start_date, expected_end_date, project_health, project_director, summary_scope,
        project_level, project_level_name, approver_authority
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, projects_data)

    # 5. MILESTONES (Dòng tiền giải ngân cấp lãnh đạo)
    milestones_data = [
        # Dự án 1: Long Thành (SBU4 - Cấp Đặc Biệt: 2.650 Tỷ)
        (1, "Nghiệm thu thảm bê tông xi măng lớp mặt đường cất hạ cánh đợt 2", (today - timedelta(days=10)).strftime(d_fmt), 20.0, 530000000000, "PAID", "ACV giải ngân đúng tiến độ"),
        (1, "Nghiệm thu hệ thống đèn tín hiệu đường băng và đường lăn kết nối", (today + timedelta(days=40)).strftime(d_fmt), 20.0, 530000000000, "INVOICED", "Đang kiểm định bay hiệu chuẩn"),

        # Dự án 2: Lạch Huyện (SBU5 - Cấp Đặc Biệt: 1.120 Tỷ)
        (2, "Nghiệm thu đóng 100% cọc ống thép SPP và bản mặt cầu cảng bến 7", (today - timedelta(days=30)).strftime(d_fmt), 35.0, 392000000000, "PAID", "Đạt nghiệm thu cảng biển"),
        (2, "Lắp đặt hệ thống đệm va và bích neo tàu 150.000 tấn", (today + timedelta(days=50)).strftime(d_fmt), 25.0, 280000000000, "PENDING", "Chờ hàng nhập khẩu cập cảng"),

        # Dự án 3: LNG Thị Vải (SBU2 - Cấp Đặc Biệt: 820 Tỷ)
        (3, "Nghiệm thu lắp đặt vỏ bồn Niken và thử áp lực thủy tĩnh", (today - timedelta(days=15)).strftime(d_fmt), 30.0, 246000000000, "PAID", "Đạt chuẩn an toàn quốc tế"),
        (3, "Nghiệm thu đấu nối đường ống công nghệ và đóng điện lạnh sâu", (today + timedelta(days=35)).strftime(d_fmt), 25.0, 205000000000, "INVOICED", "Chờ giải ngân đợt 4"),

        # Dự án 4: Grand Marina (SBU1 - Cấp 1: 480 Tỷ)
        (4, "Nghiệm thu hoàn thành hệ tường vây và đào đất hầm B3", (today - timedelta(days=20)).strftime(d_fmt), 20.0, 96000000000, "PAID", "Đã thanh toán đúng hạn"),
        (4, "Nghiệm thu đổ bê tông sàn nắp hầm B1 và đóng nắp hầm", (today + timedelta(days=25)).strftime(d_fmt), 25.0, 120000000000, "INVOICED", "Đang chuẩn bị hồ sơ nghiệm thu A-B"),

        # Dự án 7: Gemalink Cái Mép (SBU5 - Cấp 2: 220 Tỷ)
        (7, "Nghiệm thu hoàn thành ép cọc bê tông D600 cầu dẫn số 2", (today - timedelta(days=18)).strftime(d_fmt), 25.0, 55000000000, "PAID", "Nghiệm thu A-B đúng tiến độ"),

        # Dự án 9: Vành Đai 4 (SBU4 - Cấp 3: 125 Tỷ)
        (9, "Nghiệm thu thi công 500.000m cọc CDM xử lý nền đắp cao", (today - timedelta(days=12)).strftime(d_fmt), 30.0, 37500000000, "PAID", "Đạt cường độ ép mẫu R28"),

        # Dự án 11: Kè Kênh Đôi (SBU5 - Cấp 4: 38 Tỷ)
        (11, "Nghiệm thu đóng cừ bê tông dự ứng lực SW500 phân đoạn 1", (today - timedelta(days=8)).strftime(d_fmt), 30.0, 11400000000, "PAID", "Đã thanh toán kho bạc")
    ]

    cursor.executemany("""
    INSERT INTO milestones (project_id, title, due_date, percentage, amount, payment_status, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?);
    """, milestones_data)

    # 6. EXECUTIVE CARE & NETWORKING ACTIVITIES (Gắn kèm Dự án & Thẩm quyền phê duyệt cụ thể 5 Cấp)
    activities_data = [
        # Ví dụ 1: Dự án Cấp Đặc Biệt (DA 1 - Long Thành 2.650 Tỷ) -> Chủ tịch HĐQT quyết định
        (
            8, 1, "SBU4", "GIFT_DELIVERY", "Tri ân Chủ tịch HĐQT Tổng Công ty Cảng Hàng Không ACV nhân dịp vượt mốc tiến độ",
            "Ban Lãnh Đạo gửi quà và chúc mừng Chủ tịch Lại Xuân Thanh nhân dịp gói thầu 4.6 Long Thành vượt tiến độ 60 ngày. Cam kết chất lượng hạng mục đường băng cất hạ cánh số 1.",
            (today - timedelta(days=18)).strftime(d_fmt), "Chủ Tịch HĐQT & ThS. Lê Thành Trung", "SUCCESS", 10000000.0,
            "LEVEL_SPECIAL", "Chủ tịch HĐQT quyết định", "APPROVED"
        ),
        # Ví dụ 2: Dự án Cấp Đặc Biệt (DA 2 - Lạch Huyện 1.120 Tỷ) -> Chủ tịch HĐQT quyết định
        (
            10, 2, "SBU5", "EVENT_INVITATION", "Tham dự Lễ Kỷ niệm 35 Năm Ngày Truyền thống Tân Cảng Sài Gòn",
            "Đoàn đại biểu lãnh đạo do TGĐ dẫn đầu tham dự lễ kỷ niệm và chúc mừng Đại tá Ngô Minh Thuấn - Tổng Giám Đốc Tân Cảng. Hai bên thống nhất kế hoạch mở rộng gói thầu bến 7, 8.",
            (today - timedelta(days=22)).strftime(d_fmt), "Tổng Giám Đốc & KS. Trần Đình Bách", "SUCCESS", 6000000.0,
            "LEVEL_SPECIAL", "Chủ tịch HĐQT quyết định", "APPROVED"
        ),
        # Ví dụ 3: Dự án Cấp 1 (DA 4 - Grand Marina Saigon 480 Tỷ) -> Tổng Giám Đốc phê duyệt
        (
            1, 4, "SBU1", "DINNER_NETWORKING", "Tiệc tối kết nối chiến lược giữa TGĐ và Ban Lãnh Đạo Masterise Homes",
            "Tổng Giám Đốc và GĐKD SBU1 tiếp đón Phó TGĐ Masterise Phan Trọng Đạt. Hai bên thống nhất cơ chế hợp tác chuỗi 3 dự án căn hộ cao cấp ven sông chuẩn bị khởi công năm 2027.",
            (today - timedelta(days=5)).strftime(d_fmt), "Tổng Giám Đốc & KS. Đỗ Hoàng Long", "SUCCESS", 15000000.0,
            "LEVEL_1", "Tổng Giám đốc (hoặc PTGĐ có ủy quyền của Chủ tịch)", "APPROVED"
        ),
        # Ví dụ 4: Dự án Cấp 2 (DA 7 - Gemalink Cái Mép 220 Tỷ) -> Phó Tổng Giám Đốc phụ trách SBU phê duyệt
        (
            10, 7, "SBU5", "EXECUTIVE_MEETING", "Họp rà soát tiến độ bến cảng nước sâu với Ban Điều Hành Gemalink Cái Mép",
            "PTGĐ phụ trách SBU5 cùng Giám đốc Ban điều hành làm việc với đại diện liên doanh cảng. Chiêu đãi cơm trưa thân mật sau buổi kiểm tra thực địa công trường cầu cảng.",
            (today - timedelta(days=9)).strftime(d_fmt), "PTGĐ Phụ trách SBU & KS. Trần Đình Bách", "SUCCESS", 7500000.0,
            "LEVEL_2", "Phó Tổng Giám đốc phụ trách các mảng SBU", "APPROVED"
        ),
        # Ví dụ 5: Dự án Cấp 3 (DA 9 - Vành Đai 4 125 Tỷ) -> Phó Tổng Giám Đốc phụ trách SBU phê duyệt
        (
            9, 9, "SBU4", "EXECUTIVE_MEETING", "Làm việc với Lãnh đạo Ban Quản lý Dự án Đường Sắt tại Hà Nội",
            "Báo cáo kết quả thử nghiệm cọc CDM gia cố nền đất yếu dự án Vành Đai 4. Tặng quà lưu niệm và thảo luận phương án thi công thần tốc mùa mưa lũ.",
            (today - timedelta(days=14)).strftime(d_fmt), "PTGĐ Phụ trách SBU & ThS. Lê Thành Trung", "SUCCESS", 4500000.0,
            "LEVEL_3", "Phó Tổng Giám đốc phụ trách các mảng SBU", "APPROVED"
        ),
        # Ví dụ 6: Dự án Cấp 4 (DA 11 - Kè Kênh Đôi 38 Tỷ) -> Phó Tổng Giám Đốc phụ trách SBU phê duyệt
        (
            11, 11, "SBU5", "CALL_DISCUSS", "Trao đổi kỹ thuật & mời cơm trưa Đoàn thẩm định Ban QLDA Hạ Tầng Đô Thị",
            "GĐKD SBU5 cùng Phó TGĐ phụ trách tiếp đoàn công tác Sở Xây dựng và Ban QLDA kiểm tra giải pháp kè cừ dự ứng lực chống sạt lở kênh Đôi thích ứng biến đổi khí hậu.",
            (today - timedelta(days=7)).strftime(d_fmt), "PTGĐ Phụ trách SBU & KS. Trần Đình Bách", "SUCCESS", 2500000.0,
            "LEVEL_4", "Phó Tổng Giám đốc phụ trách các mảng SBU", "APPROVED"
        ),
        # Ví dụ 7: Chăm sóc chung khách hàng VIP Kim Cương (PTSC) -> Thẩm quyền Chủ tịch / TGĐ
        (
            4, 3, "SBU2", "EXECUTIVE_MEETING", "Họp chiến lược phát triển điện gió ngoài khơi với TGĐ PTSC",
            "Đoàn lãnh đạo công ty làm việc tại trụ sở PTSC. Thảo luận việc chuẩn bị bãi chế tạo chân đế điện gió xuất khẩu sang Singapore.",
            (today - timedelta(days=12)).strftime(d_fmt), "Chủ Tịch HĐQT & ThS. Nguyễn Quốc Thái", "SUCCESS", 12000000.0,
            "LEVEL_SPECIAL", "Chủ tịch HĐQT quyết định", "APPROVED"
        )
    ]

    cursor.executemany("""
    INSERT INTO customer_care_activities (
        customer_id, project_id, sbu, activity_type, title, content, occurred_at, leader_in_charge, outcome_status, cost,
        project_level, approver_authority, approval_status
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, activities_data)

    # 7. AUTOMATED EXECUTIVE MESSAGES
    messages_data = [
        (
            1, 4, "SBU1", "ZALO_ZNS", "CHUC_MUNG_SINH_NHAT", "0908112233",
            "[Zalo ZNS Lãnh Đạo] Chúc mừng Sinh nhật Phó TGĐ Phan Trọng Đạt",
            "Kính gửi Ông Phan Trọng Đạt (Phó TGĐ Masterise Homes): Nhân dịp sinh nhật của Anh, Ban Lãnh Đạo Công ty Xây dựng xin kính chúc Anh tuổi mới ngập tràn niềm vui, dồi dào sức khỏe, lãnh đạo tập đoàn ngày càng vươn xa và tiếp tục đồng hành bền chặt cùng chúng tôi!",
            "SENT", (today - timedelta(days=2)).strftime("%Y-%m-%d %H:%M:%S")
        ),
        (
            8, 1, "SBU4", "SMS", "THONG_BAO_NGHIEM_THU", "0903778899",
            "[SMS Brandname] Báo cáo mốc nghiệm thu thảm mặt đường cất hạ cánh Long Thành",
            "Kính gửi Chủ tịch ACV Lại Xuân Thanh: Hạng mục bê tông xi măng lớp mặt đường băng số 1 Long Thành đã nghiệm thu đạt 100% cường độ R28. Giá trị đề nghị giải ngân mốc 530 Tỷ VNĐ. Trân trọng báo cáo!",
            "SENT", (today - timedelta(days=10)).strftime("%Y-%m-%d %H:%M:%S")
        ),
        (
            10, 2, "SBU5", "ZALO_ZNS", "THANH_LAP_DOI_TAC", "0903991122",
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
    print("New Executive CRM seed data with 5 Project Levels & Care Approvals successfully planted!")

if __name__ == "__main__":
    seed_database()
