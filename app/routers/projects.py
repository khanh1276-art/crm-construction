"""
Executive Project & Cashflow Tracking Router for 5 SBUs.
Supports:
- Strict SBU Scope Enforcement for GĐKD SBU members (Create & Update only for their SBU)
- Admin privileges for full SBU management and Project Deletion
"""
from fastapi import APIRouter, HTTPException, Query, Header
from app.database import get_db
from app.schemas import ProjectCreate, ProjectUpdate, MilestoneCreate, MilestoneUpdate
from typing import Optional

router = APIRouter(prefix="/api/projects", tags=["Executive Project Tracking"])

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
    rows = [dict(r) for r in cursor.fetchall()]
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

    conn = get_db()
    cursor = conn.cursor()

    try:
        cursor.execute("""
        INSERT INTO projects (
            code, name, sbu, customer_id, contract_number, contract_value,
            start_date, expected_end_date, project_director, summary_scope, project_health, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'IN_PROGRESS')
        """, (
            data.code, data.name, target_sbu, data.customer_id, data.contract_number,
            data.contract_value, data.start_date, data.expected_end_date,
            data.project_director, data.summary_scope, data.project_health
        ))
        new_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {"id": new_id, "sbu": target_sbu, "message": f"Khởi tạo theo dõi dự án cho {target_sbu} thành công"}
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

    cursor.execute("SELECT sbu FROM projects WHERE id = ?", (project_id,))
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

    cursor.execute("""
    UPDATE projects SET
        name = COALESCE(?, name),
        progress_percent = COALESCE(?, progress_percent),
        paid_amount = COALESCE(?, paid_amount),
        project_health = COALESCE(?, project_health),
        status = COALESCE(?, status),
        summary_scope = COALESCE(?, summary_scope)
    WHERE id = ?
    """, (
        data.name, data.progress_percent, data.paid_amount,
        data.project_health, data.status, data.summary_scope, project_id
    ))
    conn.commit()
    conn.close()
    return {"message": "Cập nhật dự án thành công"}

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
