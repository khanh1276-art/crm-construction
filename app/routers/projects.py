"""
Executive Project & Cashflow Tracking Router for 5 SBUs.
Supports:
- Strict SBU Scope Enforcement for GĐKD SBU members (Create & Update only for their SBU)
- Admin privileges for full SBU management and Project Deletion
"""
from fastapi import APIRouter, HTTPException, Query, Header
from app.database import get_db, classify_fecon_project
from app.schemas import ProjectCreate, ProjectUpdate, MilestoneCreate, MilestoneUpdate
from typing import Optional

router = APIRouter(prefix="/api/projects", tags=["Executive Project Tracking"])

@router.get("/classification/rules")
def get_project_classification_rules():
    """
    Quy định phân loại cấp dự án FECON và thẩm quyền phê duyệt chi phí tiếp khách/CSKH:
    - Cấp Đặc Biệt (≥ 500 Tỷ VNĐ): Chủ tịch HĐQT quyết định
    - Cấp 1 (300 - < 500 Tỷ VNĐ): Tổng Giám đốc (hoặc PTGĐ có ủy quyền của Chủ tịch)
    - Cấp 2 (150 - < 300 Tỷ VNĐ): Phó Tổng Giám đốc phụ trách các mảng SBU
    - Cấp 3 (50 - < 150 Tỷ VNĐ): Phó Tổng Giám đốc phụ trách các mảng SBU
    - Cấp 4 (< 50 Tỷ VNĐ): Phó Tổng Giám đốc phụ trách các mảng SBU
    """
    return {
        "title": "Quy Định Phân Cấp Dự Án & Thẩm Quyền Phê Duyệt Chi Phí Tiếp Khách / CSKH FECON",
        "levels": [
            {
                "level": "LEVEL_SPECIAL",
                "code": "ĐẶC BIỆT",
                "name": "Dự án Cấp Đặc Biệt",
                "range": "≥ 500 Tỷ VNĐ",
                "min_value": 500000000000,
                "max_value": None,
                "approver_authority": "Chủ tịch HĐQT quyết định",
                "approver_short": "Chủ tịch HĐQT",
                "color": "purple",
                "badge": "Cấp Đặc Biệt (≥ 500 Tỷ)",
                "description": "Các dự án trọng điểm quốc gia, siêu dự án hạ tầng hoặc các hợp đồng thi công vĩ mô trên 500 tỷ đồng."
            },
            {
                "level": "LEVEL_1",
                "code": "CẤP 1",
                "name": "Dự án Cấp 1",
                "range": "Từ 300 Tỷ đến dưới 500 Tỷ VNĐ",
                "min_value": 300000000000,
                "max_value": 500000000000,
                "approver_authority": "Tổng Giám đốc (hoặc PTGĐ có ủy quyền của Chủ tịch)",
                "approver_short": "Tổng Giám đốc (hoặc PTGĐ ủy quyền)",
                "color": "rose",
                "badge": "Cấp 1 (300 - < 500 Tỷ)",
                "description": "Các dự án xây lắp quy mô lớn của các tập đoàn bất động sản hàng đầu hoặc các gói thầu hạ tầng quy mô trên 300 tỷ."
            },
            {
                "level": "LEVEL_2",
                "code": "CẤP 2",
                "name": "Dự án Cấp 2",
                "range": "Từ 150 Tỷ đến dưới 300 Tỷ VNĐ",
                "min_value": 150000000000,
                "max_value": 300000000000,
                "approver_authority": "Phó Tổng Giám đốc phụ trách các mảng SBU",
                "approver_short": "PTGĐ phụ trách SBU",
                "color": "amber",
                "badge": "Cấp 2 (150 - < 300 Tỷ)",
                "description": "Các dự án thi công cọc móng sâu, cảng biển hoặc công nghiệp có quy mô 150 đến dưới 300 tỷ đồng."
            },
            {
                "level": "LEVEL_3",
                "code": "CẤP 3",
                "name": "Dự án Cấp 3",
                "range": "Từ 50 Tỷ đến dưới 150 Tỷ VNĐ",
                "min_value": 50000000000,
                "max_value": 150000000000,
                "approver_authority": "Phó Tổng Giám đốc phụ trách các mảng SBU",
                "approver_short": "PTGĐ phụ trách SBU",
                "color": "blue",
                "badge": "Cấp 3 (50 - < 150 Tỷ)",
                "description": "Các gói thầu hạ tầng giao thông, kè chắn sóng, xử lý nền đất yếu từ 50 đến dưới 150 tỷ đồng."
            },
            {
                "level": "LEVEL_4",
                "code": "CẤP 4",
                "name": "Dự án Cấp 4",
                "range": "Dưới 50 Tỷ VNĐ",
                "min_value": 0,
                "max_value": 50000000000,
                "approver_authority": "Phó Tổng Giám đốc phụ trách các mảng SBU",
                "approver_short": "PTGĐ phụ trách SBU",
                "color": "emerald",
                "badge": "Cấp 4 (< 50 Tỷ)",
                "description": "Các gói thầu khảo sát địa kỹ thuật, thí nghiệm cọc, xử lý cục bộ dưới 50 tỷ đồng."
            }
        ]
    }

@router.get("")
def list_projects(
    sbu: Optional[str] = None,
    health: Optional[str] = None,
    search: Optional[str] = None,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    conn = get_db()
    cursor = conn.cursor()

    active_sbu = sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        active_sbu = x_user_sbu

    query = """
    SELECT p.*, c.name as customer_name, c.key_decision_maker, c.decision_maker_phone,
           (SELECT COUNT(*) FROM milestones WHERE project_id = p.id) as milestone_count
    FROM projects p
    JOIN customers c ON p.customer_id = c.id
    WHERE 1=1
    """
    params = []

    if active_sbu and active_sbu != "ALL":
        query += " AND p.sbu = ?"
        params.append(active_sbu)

    if health:
        query += " AND p.project_health = ?"
        params.append(health)

    if search:
        query += " AND (p.name LIKE ? OR p.code LIKE ? OR c.name LIKE ?)"
        s = f"%{search}%"
        params.extend([s, s, s])

    query += " ORDER BY p.id DESC"
    cursor.execute(query, params)
    rows = []
    for r in cursor.fetchall():
        item = dict(r)
        cls_info = classify_fecon_project(item.get("contract_value", 0.0))
        item["project_level"] = item.get("project_level") or cls_info["level"]
        item["project_level_name"] = item.get("project_level_name") or cls_info["level_name"]
        item["project_level_badge"] = cls_info["level_badge"]
        item["approver_authority"] = item.get("approver_authority") or cls_info["approver_authority"]
        item["approver_short"] = cls_info["approver_short"]
        item["level_color"] = cls_info["color"]
        rows.append(item)
    conn.close()
    return rows

@router.get("/{project_id}")
def get_project_detail(project_id: int):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("""
    SELECT p.*, c.name as customer_name, c.key_decision_maker, c.decision_maker_phone, c.headquarters
    FROM projects p
    JOIN customers c ON p.customer_id = c.id
    WHERE p.id = ?
    """, (project_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy dự án")

    project = dict(row)
    cls_info = classify_fecon_project(project.get("contract_value", 0.0))
    project["project_level"] = project.get("project_level") or cls_info["level"]
    project["project_level_name"] = project.get("project_level_name") or cls_info["level_name"]
    project["project_level_badge"] = cls_info["level_badge"]
    project["approver_authority"] = project.get("approver_authority") or cls_info["approver_authority"]
    project["approver_short"] = cls_info["approver_short"]
    project["level_color"] = cls_info["color"]

    # Milestones (Cashflow & Inspections)
    cursor.execute("SELECT * FROM milestones WHERE project_id = ? ORDER BY due_date ASC", (project_id,))
    project["milestones"] = [dict(r) for r in cursor.fetchall()]

    conn.close()
    return project

@router.post("")
def create_project(
    data: ProjectCreate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    if x_user_role == "COLLABORATOR":
        raise HTTPException(
            status_code=403,
            detail="Cộng tác viên (CTV) không có quyền tạo mới hồ sơ dự án & hợp đồng!"
        )

    target_sbu = data.sbu
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu and x_user_sbu != "ALL":
        target_sbu = x_user_sbu

    cls_info = classify_fecon_project(data.contract_value)
    proj_level = data.project_level or cls_info["level"]
    proj_level_name = data.project_level_name or cls_info["level_name"]
    approver = data.approver_authority or cls_info["approver_authority"]

    conn = get_db()
    cursor = conn.cursor()

    try:
        cursor.execute("""
        INSERT INTO projects (
            code, name, sbu, customer_id, contract_number, contract_value,
            start_date, expected_end_date, project_director, summary_scope, project_health, status,
            project_level, project_level_name, approver_authority
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'IN_PROGRESS', ?, ?, ?)
        """, (
            data.code, data.name, target_sbu, data.customer_id, data.contract_number,
            data.contract_value, data.start_date, data.expected_end_date,
            data.project_director, data.summary_scope, data.project_health,
            proj_level, proj_level_name, approver
        ))
        new_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {
            "id": new_id,
            "sbu": target_sbu,
            "project_level": proj_level,
            "project_level_name": proj_level_name,
            "approver_authority": approver,
            "message": f"Khởi tạo theo dõi dự án ({proj_level_name}) cho {target_sbu} thành công"
        }
    except Exception as e:
        conn.close()
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{project_id}")
def update_project(
    project_id: int,
    data: ProjectUpdate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role"),
    x_user_sbu: Optional[str] = Header("ALL", alias="X-User-SBU")
):
    if x_user_role == "COLLABORATOR":
        raise HTTPException(
            status_code=403,
            detail="Cộng tác viên (CTV) không có quyền cập nhật tiến độ & dòng tiền dự án!"
        )

    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT sbu, contract_value FROM projects WHERE id = ?", (project_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy dự án")

    proj_sbu = row["sbu"]
    if x_user_role == "SBU_DIRECTOR" and x_user_sbu != "ALL" and proj_sbu != x_user_sbu:
        conn.close()
        raise HTTPException(
            status_code=403,
            detail=f"Quyền hạn bị từ chối: GĐKD {x_user_sbu} chỉ được phép cập nhật dự án thuộc SBU của mình!"
        )

    # Re-calculate level if contract_value is updated
    new_contract_val = data.contract_value if data.contract_value is not None else row["contract_value"]
    cls_info = classify_fecon_project(new_contract_val)

    cursor.execute("""
    UPDATE projects SET
        name = COALESCE(?, name),
        progress_percent = COALESCE(?, progress_percent),
        paid_amount = COALESCE(?, paid_amount),
        contract_value = COALESCE(?, contract_value),
        project_health = COALESCE(?, project_health),
        status = COALESCE(?, status),
        summary_scope = COALESCE(?, summary_scope),
        project_level = ?,
        project_level_name = ?,
        approver_authority = ?
    WHERE id = ?
    """, (
        data.name, data.progress_percent, data.paid_amount,
        data.contract_value,
        data.project_health, data.status, data.summary_scope,
        cls_info["level"], cls_info["level_name"], cls_info["approver_authority"],
        project_id
    ))
    conn.commit()
    conn.close()
    return {
        "message": "Cập nhật dự án thành công",
        "project_level": cls_info["level"],
        "project_level_name": cls_info["level_name"],
        "approver_authority": cls_info["approver_authority"]
    }

@router.delete("/{project_id}")
def delete_project(
    project_id: int,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    # Only Admin (Ban Lãnh Đạo) has DELETE permission
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Quyền hạn bị từ chối: Chỉ có Ban Lãnh Đạo (Admin) mới có quyền xóa dự án khỏi hệ thống!"
        )

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM projects WHERE id = ?", (project_id,))
    conn.commit()
    conn.close()
    return {"message": "Đã xóa dự án thành công"}

@router.post("/{project_id}/milestones")
def add_milestone(project_id: int, data: MilestoneCreate):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO milestones (project_id, title, due_date, percentage, amount, payment_status, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """, (
        project_id, data.title, data.due_date, data.percentage, data.amount, data.payment_status, data.notes
    ))
    new_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return {"id": new_id, "message": "Đã thêm mốc giải ngân dự án"}

@router.put("/milestones/{milestone_id}")
def update_milestone(milestone_id: int, data: MilestoneUpdate):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("""
    UPDATE milestones SET
        payment_status = COALESCE(?, payment_status),
        due_date = COALESCE(?, due_date),
        notes = COALESCE(?, notes)
    WHERE id = ?
    """, (data.payment_status, data.due_date, data.notes, milestone_id))

    if data.payment_status == "PAID":
        cursor.execute("SELECT project_id FROM milestones WHERE id = ?", (milestone_id,))
        p_row = cursor.fetchone()
        if p_row:
            p_id = p_row[0]
            cursor.execute("SELECT COALESCE(SUM(amount), 0) FROM milestones WHERE project_id = ? AND payment_status = 'PAID'", (p_id,))
            paid_sum = cursor.fetchone()[0]
            cursor.execute("UPDATE projects SET paid_amount = ? WHERE id = ?", (paid_sum, p_id))

    conn.commit()
    conn.close()
    return {"message": "Cập nhật mốc giải ngân thành công"}
