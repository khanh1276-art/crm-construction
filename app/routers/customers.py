"""
Strategic Customers & Bidding Pipeline Router for 5 SBUs.
Supports:
- Intelligent Customer Deduplication & Reuse across SBUs
- Strict SBU Scope Enforcement (GĐKD SBU only manages their SBU; Admin can manage all and Delete)
"""
from fastapi import APIRouter, HTTPException, Query, Header
from app.database import get_db
from app.schemas import CustomerCreate, CustomerUpdate, BidCreate, BidStageUpdate
from typing import Optional

router = APIRouter(prefix="/api/customers", tags=["Strategic Customers & Pipeline"])

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

    try:
        cursor.execute("""
        INSERT INTO customers (
            code, name, sbu, tier, segment, tax_code, phone, email, headquarters,
            key_decision_maker, decision_maker_role, decision_maker_phone,
            decision_maker_birthday, founding_anniversary, relationship_score,
            relationship_status, strategic_notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            code, data.name, target_sbu, data.tier, data.segment, data.tax_code,
            data.phone, data.email, data.headquarters, data.key_decision_maker,
            data.decision_maker_role, data.decision_maker_phone,
            data.decision_maker_birthday, data.founding_anniversary,
            data.relationship_score, data.relationship_status, data.strategic_notes
        ))
        new_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {
            "id": new_id,
            "code": code,
            "sbu": target_sbu,
            "message": f"Lưu trữ thành công đối tác chiến lược vào khối {target_sbu}"
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
