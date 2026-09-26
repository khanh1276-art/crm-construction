"""
Executive Notification Service for 5 SBU Construction Enterprise.
Handles high-level Zalo ZNS, SMS Brandname, and Email for strategic relationships.
"""
from datetime import datetime, timedelta
from app.database import get_db

TEMPLATES = [
    {
        "id": "CHUC_MUNG_SINH_NHAT",
        "name": "Chúc mừng Sinh nhật Lãnh đạo cấp cao (Chủ tịch / TGĐ đối tác)",
        "channel": "ZALO_ZNS",
        "title": "Chúc mừng Sinh nhật [LÃNH ĐẠO ĐỐI TÁC]",
        "body": "Kính gửi [DANH XƯNG] [LÃNH ĐẠO ĐỐI TÁC] ([CHỨC VỤ] - [TÊN CÔNG TY]): Nhân dịp ngày sinh nhật, Ban Lãnh Đạo cùng toàn thể cán bộ công nhân viên Công ty Xây dựng xin trân trọng kính chúc [ANH/CHỊ] thật nhiều sức khỏe, hạnh phúc, lãnh đạo Tập đoàn gặt hái thêm nhiều thành công vượt bậc và tiếp tục hợp tác bền chặt cùng chúng tôi!",
    },
    {
        "id": "THANH_LAP_DOI_TAC",
        "name": "Chúc mừng Ngày Thành lập / Ngày Truyền thống Tập đoàn Đối tác",
        "channel": "ZALO_ZNS",
        "title": "Chúc mừng Ngày Truyền Thống [TÊN CÔNG TY]",
        "body": "Ban Lãnh Đạo Công ty Xây dựng trân trọng chúc mừng [TÊN CÔNG TY] nhân dịp kỷ niệm ngày thành lập / truyền thống. Kính chúc Quý Tập đoàn ngày càng lớn mạnh, tiếp tục khẳng định vị thế dẫn đầu và đồng hành cùng chúng tôi kiến tạo các công trình tầm vóc quốc gia!",
    },
    {
        "id": "TIEN_DO_LANH_DAO",
        "name": "Báo cáo Tiến độ Điều hành vĩ mô gửi Chủ Đầu Tư / Ban QLDA",
        "channel": "ZALO_ZNS",
        "title": "Báo cáo Tiến độ Điều hành Dự án [TÊN DỰ ÁN]",
        "body": "Kính gửi [DANH XƯNG] [LÃNH ĐẠO ĐỐI TÁC], Ban Lãnh Đạo Công ty Xây dựng trân trọng báo cáo: Dự án [TÊN DỰ ÁN] ([KHỐI SBU]) hiện đạt [TIẾN ĐỘ]% tiến độ, các mốc đường găng then chốt được kiểm soát chặt chẽ theo cam kết. Kính mời Quý Lãnh đạo tra cứu tổng thể tại cổng thông tin điều hành.",
    },
    {
        "id": "THONG_BAO_NGHIEM_THU",
        "name": "Thông báo Nghiệm thu mốc kỹ thuật & Đề nghị giải ngân",
        "channel": "SMS",
        "title": "Thông báo nghiệm thu mốc hoàn thành: [TÊN MỐC]",
        "body": "Kính gửi [LÃNH ĐẠO ĐỐI TÁC] ([TÊN CÔNG TY]): Hạng mục '[TÊN MỐC]' thuộc công trình [TÊN DỰ ÁN] đã hoàn thành đạt chuẩn kỹ thuật. Giá trị nghiệm thu đề nghị giải ngân mốc này: [SỐ TIỀN] VNĐ. Trân trọng báo cáo!",
    }
]

def send_executive_message(customer_id: int, project_id: int | None, sbu: str, channel: str, template_type: str, recipient: str, title: str, message_body: str):
    conn = get_db()
    cursor = conn.cursor()

    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
    INSERT INTO automated_messages (
        customer_id, project_id, sbu, channel, template_type, recipient, title, message_body, status, sent_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SENT', ?)
    """, (customer_id, project_id, sbu, channel, template_type, recipient, title, message_body, now_str))

    new_id = cursor.lastrowid
    conn.commit()
    conn.close()

    return {
        "id": new_id,
        "channel": channel,
        "recipient": recipient,
        "status": "SENT",
        "sent_at": now_str,
        "message": f"Đã gửi thành công qua kênh {channel}"
    }

def run_automated_executive_scan():
    conn = get_db()
    cursor = conn.cursor()
    today = datetime.now()
    generated = []

    # Check birthdays in next 5 days
    cursor.execute("SELECT id, name, sbu, key_decision_maker, decision_maker_role, decision_maker_phone, decision_maker_birthday FROM customers WHERE decision_maker_birthday IS NOT NULL AND decision_maker_phone IS NOT NULL")
    for row in cursor.fetchall():
        try:
            bday = datetime.strptime(row["decision_maker_birthday"], "%Y-%m-%d")
            this_bday = bday.replace(year=today.year)
            if this_bday.date() < today.date():
                this_bday = bday.replace(year=today.year + 1)
            days_diff = (this_bday.date() - today.date()).days
            if 0 <= days_diff <= 7:
                # Check if already sent recently
                cursor.execute("SELECT COUNT(*) FROM automated_messages WHERE customer_id = ? AND template_type = 'CHUC_MUNG_SINH_NHAT' AND sent_at >= ?", (row["id"], (today - timedelta(days=20)).strftime("%Y-%m-%d")))
                if cursor.fetchone()[0] == 0:
                    title = f"Chúc mừng Sinh nhật {row['key_decision_maker']}"
                    body = f"Kính gửi {row['key_decision_maker']} ({row['decision_maker_role']} - {row['name']}): Nhân dịp ngày sinh nhật, Ban Lãnh Đạo Công ty Xây dựng xin trân trọng kính chúc Anh/Chị tuổi mới ngập tràn niềm vui, dồi dào sức khỏe, dẫn dắt Tập đoàn gặt hái thêm nhiều thắng lợi mới!"
                    res = send_executive_message(row["id"], None, row["sbu"], "ZALO_ZNS", "CHUC_MUNG_SINH_NHAT", row["decision_maker_phone"], title, body)
                    generated.append(res)
        except Exception:
            pass

    conn.close()
    return generated
