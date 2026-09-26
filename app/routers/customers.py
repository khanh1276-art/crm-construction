"""
Strategic Customers & Bidding Pipeline Router for 5 SBUs.
Supports:
- Intelligent Customer Deduplication & Reuse across SBUs
- Strict SBU Scope Enforcement (GĐKD SBU only manages their SBU; Admin can manage all and Delete)
"""
from fastapi import APIRouter, HTTPException, Query, Header
from app.database import get_db
from app.schemas import CustomerCreate, CustomerUpdate, BidCreate, BidStageUpdate, CustomerAssessRequest
from typing import Optional

router = APIRouter(prefix="/api/customers", tags=["Strategic Customers & Pipeline"])

def evaluate_fecon_customer_tier(
    score_scale: float,
    score_fit: float,
    score_finance: float,
    score_history: float,
    score_mgmt: float,
    is_special_elevated: bool = False
):
    """
    Theo Chính sách CSKH & Đối tác FECON (FECON-CSCSKH/ĐT-01):
    - Tiêu chí 1: Quy mô Khách hàng/Đối tác, Dự án (Tối đa 15đ)
    - Tiêu chí 2: Loại hình đầu tư / Mức độ phù hợp FECON (Tối đa 25đ)
    - Tiêu chí 3: Năng lực tài chính và dòng tiền (Tối đa 25đ) - TIÊU CHÍ PHỦ QUYẾT: nếu 0đ -> tối đa Hạng Vàng
    - Tiêu chí 4: Lịch sử hợp tác và uy tín thanh toán (Tối đa 20đ)
    - Tiêu chí 5: Năng lực quản lý và tính chuyên nghiệp (Tối đa 15đ)
    Tổng: 100đ.

    Phân Hạng & Ngân sách thường niên:
    - Kim Cương (DIAMOND): >= 80đ (và không bị phủ quyết). Hạn mức 80M/năm, Chủ tịch/TGĐ trực tiếp phụ trách, 1 tháng/lần.
    - Vàng (GOLD): 50 <= điểm < 80 (hoặc >= 80 nhưng TC3 = 0). Hạn mức 20M/năm, TGĐ/SBU Leader phụ trách, 3 tháng/lần.
    - Bạc (SILVER): < 50đ. Hạn mức 5M/năm, SBU Leader/GĐKD phụ trách, Theo sự vụ thực tế.
    - Đặc cách: Nếu Chủ tịch HĐQT / TGĐ phê duyệt đặc cách -> Hạng Kim Cương.
    """
    s_scale = float(score_scale or 0.0)
    s_fit = float(score_fit or 0.0)
    s_fin = float(score_finance or 0.0)
    s_hist = float(score_history or 0.0)
    s_mgmt = float(score_mgmt or 0.0)
    total = round(s_scale + s_fit + s_fin + s_hist + s_mgmt, 1)

    if is_special_elevated:
        return {
            "tier": "DIAMOND",
            "tier_name": "Kim Cương (Đặc Cách)",
            "total_score": total,
            "veto_applied": 0,
            "annual_care_budget": 80000000.0,
            "in_charge_executive": "Chủ tịch HĐQT / TGĐ trực tiếp phụ trách (Đặc cách)",
            "care_frequency": "1 tháng / lần",
            "notes": "Được Chủ tịch HĐQT / TGĐ phê duyệt đặc cách nâng Hạng Kim Cương cho đối tác chiến lược vĩ mô"
        }

    # Tiêu chí phủ quyết (Veto Rule): Nếu Tiêu chí 3 (Tài chính) = 0 điểm
    if s_fin <= 0.0:
        if total >= 80.0:
            return {
                "tier": "GOLD",
                "tier_name": "Vàng (Áp dụng Tiêu chí Phủ Quyết)",
                "total_score": total,
                "veto_applied": 1,
                "annual_care_budget": 20000000.0,
                "in_charge_executive": "TGĐ / SBU Leader phụ trách",
                "care_frequency": "3 tháng / lần",
                "notes": "Tổng điểm ≥ 80đ nhưng bị giới hạn tối đa ở Hạng Vàng do Tiêu chí 3 (Năng lực tài chính & dòng tiền) ở mức 0 điểm (Rủi ro công nợ) theo quy định chính sách FECON."
            }
        elif total >= 50.0:
            return {
                "tier": "GOLD",
                "tier_name": "Vàng",
                "total_score": total,
                "veto_applied": 0,
                "annual_care_budget": 20000000.0,
                "in_charge_executive": "TGĐ / SBU Leader phụ trách",
                "care_frequency": "3 tháng / lần",
                "notes": "Đạt chuẩn Đối tác Quan trọng Hạng Vàng (50 - 79.9 điểm)"
            }
        else:
            return {
                "tier": "SILVER",
                "tier_name": "Bạc",
                "total_score": total,
                "veto_applied": 0,
                "annual_care_budget": 5000000.0,
                "in_charge_executive": "SBU Leader / GĐKD phụ trách",
                "care_frequency": "Theo sự vụ thực tế",
                "notes": "Đạt chuẩn Đối tác Tiềm năng Hạng Bạc (< 50 điểm)"
            }

    # Tiêu chuẩn thông thường (không bị phủ quyết)
    if total >= 80.0:
        return {
            "tier": "DIAMOND",
            "tier_name": "Kim Cương",
            "total_score": total,
            "veto_applied": 0,
            "annual_care_budget": 80000000.0,
            "in_charge_executive": "Chủ tịch HĐQT / TGĐ trực tiếp phụ trách",
            "care_frequency": "1 tháng / lần",
            "notes": "Đạt chuẩn Đối tác Chiến lược Hạng Kim Cương (≥ 80 điểm)"
        }
    elif total >= 50.0:
        return {
            "tier": "GOLD",
            "tier_name": "Vàng",
            "total_score": total,
            "veto_applied": 0,
            "annual_care_budget": 20000000.0,
            "in_charge_executive": "TGĐ / SBU Leader phụ trách",
            "care_frequency": "3 tháng / lần",
            "notes": "Đạt chuẩn Đối tác Quan trọng Hạng Vàng (50 - 79.9 điểm)"
        }
    else:
        return {
            "tier": "SILVER",
            "tier_name": "Bạc",
            "total_score": total,
            "veto_applied": 0,
            "annual_care_budget": 5000000.0,
            "in_charge_executive": "SBU Leader / GĐKD phụ trách",
            "care_frequency": "Theo sự vụ thực tế",
            "notes": "Đạt chuẩn Đối tác Tiềm năng Hạng Bạc (< 50 điểm)"
        }

@router.get("/tiers/policy")
def get_fecon_cskh_policy():
    """
    Trả về định nghĩa chi tiết Chính sách Chăm sóc Khách hàng & Phụ lục FECON (Mã: FECON-CSCSKH/ĐT-01)
    Gồm 3 Hạng (Kim Cương, Vàng, Bạc), 5 Tiêu chí đánh giá, Tiêu chí Phủ quyết và Hạn mức ngân sách.
    """
    return {
        "policy_code": "FECON-CSCSKH/ĐT-01",
        "title": "Chính Sách Chăm Sóc Khách Hàng & Đối Tác FECON",
        "tiers": [
            {
                "id": "DIAMOND",
                "name": "Kim Cương",
                "badge": "💎 Kim Cương",
                "min_score": 80,
                "max_score": 100,
                "annual_budget": 80000000,
                "annual_budget_formatted": "80.000.000 VNĐ / năm",
                "in_charge": "Chủ tịch HĐQT / TGĐ trực tiếp phụ trách",
                "frequency": "1 tháng / lần",
                "gifts_and_events": "Quà Tết Nguyên Đán đặc biệt (10M), Quà sinh nhật Chủ tịch/TGĐ đối tác (5M), Hoa chúc mừng ngày truyền thống (2M), Tiệc networking cao cấp (50-60M/năm)",
                "description": "Đối tác chiến lược tối quan trọng, có chuỗi dự án vĩ mô hoặc quan hệ quyết định đến sự phát triển của FECON."
            },
            {
                "id": "GOLD",
                "name": "Vàng",
                "badge": "🥇 Vàng",
                "min_score": 50,
                "max_score": 79.9,
                "annual_budget": 20000000,
                "annual_budget_formatted": "20.000.000 VNĐ / năm",
                "in_charge": "Tổng Giám Đốc / SBU Leader phụ trách",
                "frequency": "3 tháng / lần",
                "gifts_and_events": "Quà Tết cao cấp (5M), Quà sinh nhật Lãnh đạo đối tác (2M), Hoa chúc mừng ngày truyền thống (1M), Tiệc trao đổi dự án & giao lưu (12M/năm)",
                "description": "Đối tác quan trọng thường xuyên, có tiềm năng dự án ổn định và năng lực tài chính minh bạch."
            },
            {
                "id": "SILVER",
                "name": "Bạc",
                "badge": "🥈 Bạc",
                "min_score": 0,
                "max_score": 49.9,
                "annual_budget": 5000000,
                "annual_budget_formatted": "5.000.000 VNĐ / năm",
                "in_charge": "SBU Leader / Giám Đốc Kinh Doanh phụ trách",
                "frequency": "Theo sự vụ thực tế",
                "gifts_and_events": "Quà Tết tiêu chuẩn (2M), Hoa chúc mừng (1M), Chi phí tiếp khách theo sự vụ thực tế phát sinh (2M/năm)",
                "description": "Đối tác tiềm năng mới tiếp cận, dự án quy mô vừa và nhỏ hoặc qua kênh cộng tác viên giới thiệu."
            }
        ],
        "criteria": [
            {
                "id": "scale_project",
                "name": "Tiêu chí 1: Quy mô Khách hàng / Đối tác, Dự án",
                "weight": 15,
                "options": [
                    {"label": "Tốt (15đ): Vốn điều lệ / doanh thu > 5.000 tỷ VNĐ hoặc dự án > 1.000 tỷ VNĐ", "score": 15.0},
                    {"label": "Trung bình (7.5đ): Doanh thu 1.000 - 5.000 tỷ VNĐ hoặc dự án 200 - 1.000 tỷ VNĐ", "score": 7.5},
                    {"label": "Tiềm năng (0đ): Doanh nghiệp quy mô nhỏ / dự án dưới 200 tỷ VNĐ", "score": 0.0}
                ]
            },
            {
                "id": "fecon_fit",
                "name": "Tiêu chí 2: Loại hình đầu tư / Mức độ phù hợp FECON",
                "weight": 25,
                "options": [
                    {"label": "Tốt (25đ): Rất phù hợp - Thuộc các lĩnh vực lõi (Nền móng ngầm, Metro, Cảng biển, Năng lượng)", "score": 25.0},
                    {"label": "Trung bình (12.5đ): Phù hợp vừa phải - Công trình dân dụng, công nghiệp thông thường", "score": 12.5},
                    {"label": "Chưa phù hợp (0đ): Không thuộc định hướng công nghệ hay thế mạnh thi công của FECON", "score": 0.0}
                ]
            },
            {
                "id": "financial_capacity",
                "name": "Tiêu chí 3: Năng lực tài chính và dòng tiền (TIÊU CHÍ PHỦ QUYẾT)",
                "weight": 25,
                "is_veto": True,
                "veto_warning": "⚠️ QUY TẮC PHỦ QUYẾT (VETO): Nếu tiêu chí này ở mức Tiềm năng (0đ), đối tác BỊ GIỚI HẠN TỐI ĐA Ở HẠNG VÀNG kể cả tổng điểm ≥ 80đ để phòng ngừa rủi ro nợ đọng công nợ!",
                "options": [
                    {"label": "Tốt (25đ): Năng lực tài chính rất mạnh, dòng tiền minh bạch, nguồn vốn FDI/ODA/Ngân sách/Vốn tự có lớn", "score": 25.0},
                    {"label": "Trung bình (12.5đ): Tài chính ổn định, phụ thuộc vốn vay thương mại hoặc giải ngân theo tiến độ", "score": 12.5},
                    {"label": "Tiềm năng / Rủi ro (0đ): Dòng tiền yếu, nợ đọng, rủi ro thu hồi công nợ cao (KÍCH HOẠT PHỦ QUYẾT)", "score": 0.0}
                ]
            },
            {
                "id": "cooperation_history",
                "name": "Tiêu chí 4: Lịch sử hợp tác và uy tín thanh toán",
                "weight": 20,
                "options": [
                    {"label": "Tốt (20đ): Đã từng hợp tác thành công, thanh toán đúng hạn, phối hợp công trường xuất sắc", "score": 20.0},
                    {"label": "Trung bình (10đ): Đã hợp tác nhưng còn chậm thanh toán hoặc đối tác mới có uy tín thị trường tốt", "score": 10.0},
                    {"label": "Tiềm năng (0đ): Đối tác mới chưa kiểm chứng hoặc từng có tranh chấp, chậm thanh toán kéo dài", "score": 0.0}
                ]
            },
            {
                "id": "management_capacity",
                "name": "Tiêu chí 5: Năng lực quản lý và tính chuyên nghiệp",
                "weight": 15,
                "options": [
                    {"label": "Tốt (15đ): Bộ máy chuyên nghiệp, tiêu chuẩn HSE/Kỹ thuật khắt khe, quyết định nhanh, minh bạch", "score": 15.0},
                    {"label": "Trung bình (7.5đ): Bộ máy vừa phải, quy trình phê duyệt nhiều cấp, cần nhiều thời gian ra quyết định", "score": 7.5},
                    {"label": "Tiềm năng (0đ): Bộ máy cồng kềnh, thiếu nhất quán, khó tiếp cận cấp quyết định then chốt", "score": 0.0}
                ]
            }
        ],
        "special_elevation_rule": "Chủ tịch HĐQT hoặc Tổng Giám Đốc có quyền phê duyệt đặc cách xếp Hạng Kim Cương cho đối tác chiến lược vĩ mô bất kể điểm số đánh giá."
    }

# --- Customer Duplicate Check & Auto-Reuse ---
@router.get("/check-duplicate")
def check_duplicate_customer(
    tax_code: Optional[str] = Query(None),
    phone: Optional[str] = Query(None),
    name: Optional[str] = Query(None)
):
    """
    Scans the database across all SBUs to find matching corporate clients.
    Allows reusing verified customer info, tax code, and decision maker profiles.
    """
    if not tax_code and not phone and not name:
        return {"found": False, "matches": []}

    conn = get_db()
    cursor = conn.cursor()

    conditions = []
    params = []

    if tax_code and len(tax_code.strip()) >= 5:
        conditions.append("tax_code = ?")
        params.append(tax_code.strip())

    if phone and len(phone.strip()) >= 8:
        conditions.append("(phone = ? OR decision_maker_phone = ?)")
        params.extend([phone.strip(), phone.strip()])

    if name and len(name.strip()) >= 4:
        conditions.append("name LIKE ?")
        params.append(f"%{name.strip()}%")

    if not conditions:
        conn.close()
        return {"found": False, "matches": []}

    query = f"""
    SELECT id, code, name, sbu, tier, segment, tax_code, phone, email, headquarters,
           key_decision_maker, decision_maker_role, decision_maker_phone,
           decision_maker_birthday, founding_anniversary, relationship_score, strategic_notes
    FROM customers
    WHERE {' OR '.join(conditions)}
    LIMIT 5
    """
    cursor.execute(query, params)
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()

    return {
        "found": len(rows) > 0,
        "count": len(rows),
        "matches": rows
    }

@router.get("")
def list_customers(
    sbu: Optional[str] = None,
    tier: Optional[str] = None,
    search: Optional[str] = None,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    conn = get_db()
    cursor = conn.cursor()

    query = """
    SELECT c.*,
           COUNT(DISTINCT p.id) as project_count,
           COALESCE(SUM(p.contract_value), 0) as total_contract_value
    FROM customers c
    LEFT JOIN projects p ON c.id = p.customer_id
    WHERE 1=1
    """
    params = []

    # Enforce SBU scope if GĐKD
    active_sbu = sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        active_sbu = x_user_sbu

    if active_sbu and active_sbu != "ALL":
        query += " AND c.sbu = ?"
        params.append(active_sbu)

    if tier:
        if tier in ('DIAMOND', 'STRATEGIC_VIP'):
            query += " AND c.tier IN ('DIAMOND', 'STRATEGIC_VIP')"
        elif tier in ('GOLD', 'CLOSE_PARTNER'):
            query += " AND c.tier IN ('GOLD', 'CLOSE_PARTNER')"
        elif tier in ('SILVER', 'PROSPECT'):
            query += " AND c.tier IN ('SILVER', 'PROSPECT')"
        else:
            query += " AND c.tier = ?"
            params.append(tier)

    if search:
        query += " AND (c.name LIKE ? OR c.key_decision_maker LIKE ? OR c.code LIKE ? OR c.phone LIKE ? OR c.tax_code LIKE ?)"
        s = f"%{search}%"
        params.extend([s, s, s, s, s])

    query += " GROUP BY c.id ORDER BY c.id DESC"
    cursor.execute(query, params)
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

@router.post("")
def create_customer(
    data: CustomerCreate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    # Enforce SBU constraint for SBU Directors
    target_sbu = data.sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        target_sbu = x_user_sbu

    conn = get_db()
    cursor = conn.cursor()

    # Generate customer code
    code = data.code
    if not code:
        prefix = f"KH-{target_sbu}-"
        cursor.execute("SELECT COUNT(*) FROM customers WHERE code LIKE ?", (f"{prefix}%",))
        cnt = cursor.fetchone()[0] + 1
        code = f"{prefix}{cnt:02d}"

    # Evaluate tier according to FECON 5-criteria policy
    eval_res = evaluate_fecon_customer_tier(
        score_scale=data.score_scale_project or 15.0,
        score_fit=data.score_fecon_fit or 25.0,
        score_finance=data.score_financial_capacity or 25.0,
        score_history=data.score_cooperation_history or 20.0,
        score_mgmt=data.score_management_capacity or 15.0,
        is_special_elevated=bool(data.is_special_elevated)
    )

    assigned_tier = eval_res["tier"] if data.tier in ('GOLD', 'STRATEGIC_VIP', None) else data.tier
    annual_budget = eval_res["annual_care_budget"]
    in_charge = eval_res["in_charge_executive"]
    frequency = eval_res["care_frequency"]
    total_score = eval_res["total_score"]
    veto_applied = eval_res["veto_applied"]

    try:
        cursor.execute("""
        INSERT INTO customers (
            code, name, sbu, tier, segment, tax_code, phone, email, headquarters,
            key_decision_maker, decision_maker_role, decision_maker_phone,
            decision_maker_birthday, founding_anniversary, relationship_score,
            relationship_status, strategic_notes,
            score_scale_project, score_fecon_fit, score_financial_capacity,
            score_cooperation_history, score_management_capacity, total_score,
            is_special_elevated, veto_applied, annual_care_budget, spent_care_budget,
            in_charge_executive, care_frequency
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            code, data.name, target_sbu, assigned_tier, data.segment, data.tax_code,
            data.phone, data.email, data.headquarters, data.key_decision_maker,
            data.decision_maker_role, data.decision_maker_phone,
            data.decision_maker_birthday, data.founding_anniversary,
            data.relationship_score, data.relationship_status, data.strategic_notes,
            data.score_scale_project or 15.0, data.score_fecon_fit or 25.0,
            data.score_financial_capacity or 25.0, data.score_cooperation_history or 20.0,
            data.score_management_capacity or 15.0, total_score,
            1 if data.is_special_elevated else 0, veto_applied,
            annual_budget, float(data.spent_care_budget or 0.0),
            in_charge, frequency
        ))
        new_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {
            "id": new_id,
            "code": code,
            "sbu": target_sbu,
            "tier": assigned_tier,
            "total_score": total_score,
            "annual_care_budget": annual_budget,
            "in_charge_executive": in_charge,
            "message": f"Lưu trữ thành công đối tác chiến lược vào khối {target_sbu} ({eval_res['tier_name']})"
        }
    except Exception as e:
        conn.close()
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/{customer_id}")
def get_customer_detail(customer_id: int):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM customers WHERE id = ?", (customer_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy khách hàng")
    customer = dict(row)

    # Related Projects
    cursor.execute("SELECT * FROM projects WHERE customer_id = ? ORDER BY id DESC", (customer_id,))
    customer["projects"] = [dict(r) for r in cursor.fetchall()]

    # Related Pipeline Bids
    cursor.execute("SELECT * FROM pipeline_bids WHERE customer_id = ? ORDER BY id DESC", (customer_id,))
    customer["bids"] = [dict(r) for r in cursor.fetchall()]

    # Executive Care Activities
    cursor.execute("""
    SELECT * FROM customer_care_activities
    WHERE customer_id = ?
    ORDER BY occurred_at DESC, id DESC
    """, (customer_id,))
    customer["activities"] = [dict(r) for r in cursor.fetchall()]

    # Executive Messages Sent
    cursor.execute("""
    SELECT * FROM automated_messages
    WHERE customer_id = ?
    ORDER BY id DESC
    """, (customer_id,))
    customer["messages"] = [dict(r) for r in cursor.fetchall()]

    conn.close()
    return customer

@router.put("/{customer_id}")
def update_customer(
    customer_id: int,
    data: CustomerUpdate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    conn = get_db()
    cursor = conn.cursor()

    # Permission check: Collaborator cannot edit, SBU Director can only update customers in their own SBU
    if x_user_role == "COLLABORATOR":
        conn.close()
        raise HTTPException(
            status_code=403,
            detail="Cộng tác viên (CTV) chỉ được phép giới thiệu & thêm mới đối tác, không được quyền chỉnh sửa hồ sơ đối tác chiến lược!"
        )

    cursor.execute("SELECT sbu FROM customers WHERE id = ?", (customer_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy khách hàng")

    cust_sbu = row["sbu"]
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu != "ALL" and cust_sbu != x_user_sbu:
        conn.close()
        raise HTTPException(
            status_code=403,
            detail=f"Quyền hạn bị từ chối: GĐKD {x_user_sbu} chỉ được phép cập nhật khách hàng thuộc SBU của mình!"
        )

    target_sbu = cust_sbu if x_user_role == "SBU_DIRECTOR" else data.sbu

    cursor.execute("""
    UPDATE customers SET
        name = ?, sbu = ?, tier = ?, segment = ?, tax_code = ?, phone = ?, email = ?,
        headquarters = ?, key_decision_maker = ?, decision_maker_role = ?,
        decision_maker_phone = ?, decision_maker_birthday = ?, founding_anniversary = ?,
        relationship_score = ?, relationship_status = ?, strategic_notes = ?,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = ?
    """, (
        data.name, target_sbu, data.tier, data.segment, data.tax_code, data.phone, data.email,
        data.headquarters, data.key_decision_maker, data.decision_maker_role,
        data.decision_maker_phone, data.decision_maker_birthday, data.founding_anniversary,
        data.relationship_score, data.relationship_status, data.strategic_notes,
        customer_id
    ))
    conn.commit()
    conn.close()
    return {"message": "Cập nhật thông tin đối tác chiến lược thành công"}

@router.post("/{customer_id}/assess")
def assess_customer(
    customer_id: int,
    data: CustomerAssessRequest,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    """
    Đánh giá & Phân hạng Khách hàng theo 5 Tiêu chí FECON (Mã: FECON-CSCSKH/ĐT-01):
    1. Quy mô (Max 15)
    2. Phù hợp FECON (Max 25)
    3. Năng lực tài chính & dòng tiền (Max 25 - VETO)
    4. Lịch sử hợp tác & uy tín (Max 20)
    5. Năng lực quản lý (Max 15)
    Tự động áp dụng quy tắc phủ quyết (TC3 = 0 -> tối đa Hạng Vàng)
    hoặc phê duyệt đặc cách Hạng Kim Cương từ CT HĐQT/TGĐ.
    Cập nhật hạn mức ngân sách và phân công lãnh đạo phụ trách.
    """
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM customers WHERE id = ?", (customer_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy khách hàng")

    eval_res = evaluate_fecon_customer_tier(
        score_scale=data.score_scale_project,
        score_fit=data.score_fecon_fit,
        score_finance=data.score_financial_capacity,
        score_history=data.score_cooperation_history,
        score_mgmt=data.score_management_capacity,
        is_special_elevated=data.is_special_elevated
    )

    new_tier = eval_res["tier"]
    total_score = eval_res["total_score"]
    veto_applied = eval_res["veto_applied"]
    is_special = 1 if data.is_special_elevated else 0
    annual_budget = eval_res["annual_care_budget"]
    in_charge = eval_res["in_charge_executive"]
    frequency = eval_res["care_frequency"]

    cursor.execute("""
    UPDATE customers SET
        score_scale_project = ?,
        score_fecon_fit = ?,
        score_financial_capacity = ?,
        score_cooperation_history = ?,
        score_management_capacity = ?,
        total_score = ?,
        is_special_elevated = ?,
        veto_applied = ?,
        tier = ?,
        annual_care_budget = ?,
        in_charge_executive = ?,
        care_frequency = ?,
        strategic_notes = COALESCE(?, strategic_notes),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = ?
    """, (
        data.score_scale_project,
        data.score_fecon_fit,
        data.score_financial_capacity,
        data.score_cooperation_history,
        data.score_management_capacity,
        total_score,
        is_special,
        veto_applied,
        new_tier,
        annual_budget,
        in_charge,
        frequency,
        data.strategic_notes,
        customer_id
    ))
    conn.commit()
    conn.close()

    return {
        "customer_id": customer_id,
        "evaluation": eval_res,
        "message": f"Chấm điểm thành công: Tổng {total_score}/100đ -> Xếp {eval_res['tier_name']}"
    }

@router.delete("/{customer_id}")
def delete_customer(
    customer_id: int,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    # Only Admin (Ban Lãnh Đạo) has DELETE permission
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Quyền hạn bị từ chối: Chỉ có Ban Lãnh Đạo (Admin) mới có quyền xóa khách hàng khỏi hệ thống!"
        )

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM customers WHERE id = ?", (customer_id,))
    conn.commit()
    conn.close()
    return {"message": "Đã xóa khách hàng thành công"}

# --- Bidding Pipeline Endpoints ---
@router.get("/bids/pipeline")
def get_bids_pipeline(
    sbu: Optional[str] = None,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    conn = get_db()
    cursor = conn.cursor()

    active_sbu = sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        active_sbu = x_user_sbu

    query = """
    SELECT b.*, c.name as customer_name, c.key_decision_maker, c.decision_maker_phone
    FROM pipeline_bids b
    JOIN customers c ON b.customer_id = c.id
    WHERE 1=1
    """
    params = []
    if active_sbu and active_sbu != "ALL":
        query += " AND b.sbu = ?"
        params.append(active_sbu)

    query += " ORDER BY b.id DESC"
    cursor.execute(query, params)
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()

    stages = [
        {"id": "INFORMATION", "label": "1. Tiếp cận thông tin sơ bộ"},
        {"id": "EVALUATION", "label": "2. Khảo sát & Đánh giá"},
        {"id": "TENDER_PREP", "label": "3. Lập hồ sơ thầu / Báo giá"},
        {"id": "NEGOTIATION", "label": "4. Thương thảo hợp đồng"},
        {"id": "WON", "label": "5. Trúng thầu (Ký HĐ)"},
        {"id": "LOST", "label": "6. Trượt thầu / Tạm dừng"}
    ]

    grouped = {s["id"]: [] for s in stages}
    for row in rows:
        stg = row.get("stage")
        if stg in grouped:
            grouped[stg].append(row)
        else:
            grouped["INFORMATION"].append(row)

    return {
        "stages": stages,
        "items": grouped,
        "total_count": len(rows)
    }

@router.post("/bids")
def create_bid(
    data: BidCreate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    target_sbu = data.sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        target_sbu = x_user_sbu

    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO pipeline_bids (
        customer_id, sbu, project_title, estimated_value, stage, win_rate,
        tender_deadline, target_kickoff, assigned_director, bidding_notes
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        data.customer_id, target_sbu, data.project_title, data.estimated_value,
        data.stage, data.win_rate, data.tender_deadline, data.target_kickoff,
        data.assigned_director, data.bidding_notes
    ))
    new_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return {"id": new_id, "message": "Thêm hồ sơ cơ hội / dự thầu thành công"}

@router.get("/bids/{bid_id}")
def get_bid_detail(bid_id: int):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT b.*, c.name as customer_name, c.key_decision_maker, c.decision_maker_phone, c.decision_maker_role
    FROM pipeline_bids b
    JOIN customers c ON b.customer_id = c.id
    WHERE b.id = ?
    """, (bid_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        raise HTTPException(status_code=404, detail="Không tìm thấy gói thầu")
    return dict(row)

@router.put("/bids/{bid_id}/stage")
def update_bid_stage(
    bid_id: int,
    data: BidStageUpdate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT sbu FROM pipeline_bids WHERE id = ?", (bid_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy gói thầu")

    if x_user_role == "SBU_DIRECTOR" and x_user_sbu != "ALL" and row["sbu"] != x_user_sbu:
        conn.close()
        raise HTTPException(
            status_code=403,
            detail=f"GĐKD {x_user_sbu} chỉ được cập nhật gói thầu thuộc SBU của mình!"
        )

    cursor.execute("""
    UPDATE pipeline_bids
    SET stage = ?, win_rate = COALESCE(?, win_rate), bidding_notes = COALESCE(?, bidding_notes)
    WHERE id = ?
    """, (data.stage, data.win_rate, data.bidding_notes, bid_id))
    conn.commit()
    conn.close()
    return {"message": "Cập nhật tiến trình đấu thầu thành công"}

@router.delete("/bids/{bid_id}")
def delete_bid(
    bid_id: int,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Chỉ có Ban Lãnh Đạo (Admin) mới có quyền xóa hồ sơ thầu!"
        )

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM pipeline_bids WHERE id = ?", (bid_id,))
    conn.commit()
    conn.close()
    return {"message": "Đã xóa hồ sơ thầu"}
