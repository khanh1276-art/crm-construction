"""
Executive Customer Care & Networking Activities Router for 5 SBUs.
Tracks Chairman / CEO / SBU Director meetings, dinners, formal event invitations, and gifts.
Integrates Project Level & Approver Authority classification.
"""
from fastapi import APIRouter, HTTPException, Query
from app.database import get_db, classify_fecon_project
from app.schemas import CareActivityCreate
from typing import Optional

router = APIRouter(prefix="/api/care-activities", tags=["Customer Care & Executive Relations"])

@router.get("")
def list_care_activities(
    sbu: Optional[str] = None,
    customer_id: Optional[int] = None,
    project_id: Optional[int] = None,
    limit: int = 50
):
    conn = get_db()
    cursor = conn.cursor()

    query = """
    SELECT a.*, 
           c.name as customer_name, c.key_decision_maker, c.decision_maker_role, c.decision_maker_phone, c.tier as customer_tier,
           p.name as project_name, p.code as project_code, p.contract_value as project_contract_value
    FROM customer_care_activities a
    JOIN customers c ON a.customer_id = c.id
    LEFT JOIN projects p ON a.project_id = p.id
    WHERE 1=1
    """
    params = []

    if sbu and sbu != "ALL":
        query += " AND a.sbu = ?"
        params.append(sbu)

    if customer_id:
        query += " AND a.customer_id = ?"
        params.append(customer_id)

    if project_id:
        query += " AND a.project_id = ?"
        params.append(project_id)

    query += " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ?"
    params.append(limit)

    cursor.execute(query, params)
    raw_rows = [dict(r) for r in cursor.fetchall()]
    conn.close()

    rows = []
    for r in raw_rows:
        proj_val = r.get("project_contract_value")
        if proj_val is not None and proj_val > 0:
            cls_info = classify_fecon_project(proj_val)
            r["project_level"] = r.get("project_level") or cls_info["level"]
            r["project_level_name"] = cls_info["level_name"]
            r["project_level_badge"] = cls_info["level_badge"]
            r["approver_authority"] = r.get("approver_authority") or cls_info["approver_authority"]
            r["approver_short"] = cls_info["approver_short"]
            r["level_color"] = cls_info["color"]
        else:
            # Fallback to customer tier policy if not project-specific
            tier = r.get("customer_tier")
            if tier == "DIAMOND":
                r["project_level"] = "DIAMOND_CARE"
                r["project_level_name"] = "Khách hàng Kim Cương"
                r["project_level_badge"] = "💎 Kim Cương"
                r["approver_authority"] = r.get("approver_authority") or "Chủ tịch HĐQT / TGĐ trực tiếp duyệt"
                r["approver_short"] = "Chủ tịch / TGĐ"
                r["level_color"] = "purple"
            elif tier == "GOLD":
                r["project_level"] = "GOLD_CARE"
                r["project_level_name"] = "Khách hàng Hạng Vàng"
                r["project_level_badge"] = "🥇 Hạng Vàng"
                r["approver_authority"] = r.get("approver_authority") or "TGĐ / SBU Leader phụ trách phê duyệt"
                r["approver_short"] = "TGĐ / SBU Leader"
                r["level_color"] = "amber"
            else:
                r["project_level"] = "SILVER_CARE"
                r["project_level_name"] = "Khách hàng Hạng Bạc"
                r["project_level_badge"] = "🥈 Hạng Bạc"
                r["approver_authority"] = r.get("approver_authority") or "SBU Leader / GĐKD phụ trách phê duyệt"
                r["approver_short"] = "SBU Leader / GĐKD"
                r["level_color"] = "slate"
        rows.append(r)

    return rows

@router.post("")
def create_care_activity(data: CareActivityCreate):
    conn = get_db()
    cursor = conn.cursor()

    try:
        activity_cost = float(data.cost or 0.0)
        proj_level = data.project_level
        approver = data.approver_authority

        if data.project_id:
            cursor.execute("SELECT contract_value, name, code FROM projects WHERE id = ?", (data.project_id,))
            p_row = cursor.fetchone()
            if p_row:
                cls_info = classify_fecon_project(p_row[0])
                if not proj_level:
                    proj_level = cls_info["level"]
                if not approver:
                    approver = cls_info["approver_authority"]
        
        if not approver:
            # Query customer tier
            cursor.execute("SELECT tier FROM customers WHERE id = ?", (data.customer_id,))
            c_row = cursor.fetchone()
            tier = c_row[0] if c_row else "GOLD"
            if tier == "DIAMOND":
                approver = "Chủ tịch HĐQT / TGĐ trực tiếp duyệt"
                proj_level = proj_level or "DIAMOND_CARE"
            elif tier == "GOLD":
                approver = "TGĐ / SBU Leader phụ trách phê duyệt"
                proj_level = proj_level or "GOLD_CARE"
            else:
                approver = "SBU Leader / GĐKD phụ trách phê duyệt"
                proj_level = proj_level or "SILVER_CARE"

        approval_status = data.approval_status or "APPROVED"

        cursor.execute("""
        INSERT INTO customer_care_activities (
            customer_id, project_id, sbu, activity_type, title, content, occurred_at, leader_in_charge, outcome_status, cost,
            project_level, approver_authority, approval_status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            data.customer_id, data.project_id, data.sbu, data.activity_type, data.title,
            data.content, data.occurred_at, data.leader_in_charge, data.outcome_status,
            activity_cost, proj_level, approver, approval_status
        ))
        new_id = cursor.lastrowid

        # Automatically update spent_care_budget for customer if cost > 0
        if activity_cost > 0:
            cursor.execute("""
            UPDATE customers
            SET spent_care_budget = COALESCE(spent_care_budget, 0) + ?
            WHERE id = ?
            """, (activity_cost, data.customer_id))

        conn.commit()
        conn.close()
        return {
            "id": new_id,
            "project_level": proj_level,
            "approver_authority": approver,
            "approval_status": approval_status,
            "message": "Ghi nhận hoạt động chăm sóc & ngoại giao thành công"
        }
    except Exception as e:
        conn.close()
        raise HTTPException(status_code=400, detail=str(e))
