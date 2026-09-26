"""
Database setup for Executive Construction CRM tailored for:
- Leadership (Ban Lãnh Đạo - Admin)
- 5 SBU Business Development Directors:
  1. SBU1 - Nền móng và Hầm
  2. SBU2 - Xây dựng Năng lượng và công nghiệp
  3. SBU3 - Metro và ngầm đô thị
  4. SBU4 - Hạ tầng tập trung và đường sắt cao tốc
  5. SBU5 - Cảng biển và biến đổi khí hậu
"""
import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "crm_construction.db"

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()

    # 1. Users / Accounts (Ban Lãnh Đạo, 5 GĐKD SBU, và Cộng Tác Viên)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password TEXT DEFAULT '123456',
        full_name TEXT NOT NULL,
        role TEXT NOT NULL, -- ADMIN (Ban Lãnh Đạo), SBU_DIRECTOR (GĐKD SBU), COLLABORATOR (Cộng Tác Viên)
        sbu TEXT NOT NULL,  -- ALL, SBU1, SBU2, SBU3, SBU4, SBU5
        title TEXT NOT NULL,
        email TEXT,
        phone TEXT,
        avatar_icon TEXT DEFAULT 'fa-user-tie'
    );
    """)

    # Migration check for password column
    cursor.execute("PRAGMA table_info(users);")
    user_cols = [col[1] for col in cursor.fetchall()]
    if "password" not in user_cols:
        cursor.execute("ALTER TABLE users ADD COLUMN password TEXT DEFAULT '123456';")

    # 2. Customers & Strategic Stakeholders (Chủ đầu tư, Ban QLDA, Tập đoàn)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS customers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        sbu TEXT NOT NULL, -- SBU1, SBU2, SBU3, SBU4, SBU5
        tier TEXT NOT NULL DEFAULT 'STRATEGIC_VIP', -- STRATEGIC_VIP, CLOSE_PARTNER, PROSPECT
        segment TEXT NOT NULL DEFAULT 'B2B', -- B2B, B2G, FDI
        tax_code TEXT,
        phone TEXT,
        email TEXT,
        headquarters TEXT,
        key_decision_maker TEXT NOT NULL, -- Chủ tịch, TGĐ, Trưởng ban QLDA
        decision_maker_role TEXT,
        decision_maker_phone TEXT,
        decision_maker_birthday TEXT, -- YYYY-MM-DD
        founding_anniversary TEXT, -- Ngày thành lập công ty / Ngày truyền thống
        relationship_score INTEGER DEFAULT 5, -- 1-5 sao
        relationship_status TEXT DEFAULT 'EXCELLENT', -- EXCELLENT, STABLE, NEEDS_ATTENTION
        strategic_notes TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    );
    """)

    # 3. Bidding & Opportunity Pipeline (Phễu cơ hội & Hồ sơ dự thầu)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS pipeline_bids (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customer_id INTEGER NOT NULL,
        sbu TEXT NOT NULL, -- SBU1 to SBU5
        project_title TEXT NOT NULL,
        estimated_value REAL DEFAULT 0,
        stage TEXT NOT NULL DEFAULT 'INFORMATION', 
        -- INFORMATION (1. Tiếp cận thông tin sơ bộ)
        -- EVALUATION (2. Khảo sát & Đánh giá năng lực)
        -- TENDER_PREP (3. Lập hồ sơ dự thầu & Báo giá)
        -- NEGOTIATION (4. Thương thảo thương mại & Kỹ thuật)
        -- WON (5. Trúng thầu / Ký kết HĐ)
        -- LOST (6. Trượt thầu / Tạm dừng)
        win_rate INTEGER DEFAULT 50, -- %
        tender_deadline TEXT,
        target_kickoff TEXT,
        assigned_director TEXT,
        bidding_notes TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
    );
    """)

    # 4. Projects (Theo dõi vĩ mô dự án & Dòng tiền cho Ban Lãnh Đạo & GĐKD)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        sbu TEXT NOT NULL, -- SBU1 to SBU5
        customer_id INTEGER NOT NULL,
        contract_number TEXT,
        contract_value REAL DEFAULT 0,
        paid_amount REAL DEFAULT 0,
        progress_percent REAL DEFAULT 0,
        start_date TEXT,
        expected_end_date TEXT,
        project_health TEXT DEFAULT 'GOOD', -- GOOD (Xanh), ATTENTION (Vàng), DELAYED (Đỏ)
        project_director TEXT,
        summary_scope TEXT,
        status TEXT NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, WARRANTY
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
    );
    """)

    # 5. Milestones (Mốc nghiệm thu & Dòng tiền thanh toán cấp lãnh đạo)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS milestones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_id INTEGER NOT NULL,
        title TEXT NOT NULL,
        due_date TEXT,
        percentage REAL DEFAULT 0,
        amount REAL DEFAULT 0,
        payment_status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, INVOICED, PAID, OVERDUE
        notes TEXT,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
    );
    """)

    # 6. Customer Care & Executive Relations (Nhật ký chăm sóc & Ngoại giao cấp cao)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS customer_care_activities (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customer_id INTEGER NOT NULL,
        sbu TEXT NOT NULL,
        activity_type TEXT NOT NULL, -- EXECUTIVE_MEETING, DINNER_NETWORKING, EVENT_INVITATION, GIFT_DELIVERY, CALL_DISCUSS
        title TEXT NOT NULL,
        content TEXT,
        occurred_at TEXT NOT NULL,
        leader_in_charge TEXT NOT NULL,
        outcome_status TEXT DEFAULT 'SUCCESS',
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
    );
    """)

    # 7. Automated Executive Messages (Zalo ZNS / SMS / Email trang trọng)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS automated_messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customer_id INTEGER NOT NULL,
        project_id INTEGER,
        sbu TEXT NOT NULL,
        channel TEXT NOT NULL, -- ZALO_ZNS, SMS, EMAIL
        template_type TEXT NOT NULL, -- CHUC_MUNG_SINH_NHAT, THANH_LAP_DOI_TAC, TIEN_DO_LANH_DAO, THONG_BAO_NGHIEM_THU
        recipient TEXT NOT NULL,
        title TEXT NOT NULL,
        message_body TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'SENT', -- SENT, SCHEDULED, FAILED
        sent_at TEXT DEFAULT CURRENT_TIMESTAMP,
        metadata_json TEXT,
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
    );
    """)

    conn.commit()
    
    # Auto-seed if database is fresh / empty
    cursor.execute("SELECT COUNT(*) FROM users;")
    count = cursor.fetchone()[0]
    conn.close()

    if count == 0:
        try:
            from app.services.seed_data import seed_database
            seed_database()
        except Exception as e:
            print(f"Warning: Failed to auto-seed database: {e}")

if __name__ == "__main__":
    init_db()
    print("Database initialized successfully!")
