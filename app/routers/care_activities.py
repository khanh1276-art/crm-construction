"""
Executive Customer Care & Networking Activities Router for 5 SBUs.
Tracks Chairman / CEO / SBU Director meetings, dinners, formal event invitations, and gifts.
"""
from fastapi import APIRouter, HTTPException, Query
from app.database import get_db
from app.schemas import CareActivityCreate
from typing import Optional

router = APIRouter(prefix="/api/care-activities", tags=["Customer Care & Executive Relations"])

@router.get("")
def list_care_activities(
    sbu: Optional[str] = None,
    customer_id: Optional[int] = None,
    limit: int = 30
):
    conn = get_db()
    cursor = conn.cursor()

    query = """
    SELECT a.*, c.name as customer_name, c.key_decision_maker, c.decision_maker_role, c.decision_maker_phone
    FROM customer_care_activities a
    JOIN customers c ON a.customer_id = c.id
    WHERE 1=1
    """
    params = []

    if sbu and sbu != "ALL":
        query += " AND a.sbu = ?"
        params.append(sbu)

    if customer_id:
        query += " AND a.customer_id = ?"
        params.append(customer_id)

    query += " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ?"
    params.append(limit)

    cursor.execute(query, params)
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

@router.post("")
def create_care_activity(data: CareActivityCreate):
    conn = get_db()
    cursor = conn.cursor()

    try:
        activity_cost = float(data.cost or 0.0)
        cursor.execute("""
        INSERT INTO customer_care_activities (
            customer_id, sbu, activity_type, title, content, occurred_at, leader_in_charge, outcome_status, cost
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            data.customer_id, data.sbu, data.activity_type, data.title,
            data.content, data.occurred_at, data.leader_in_charge, data.outcome_status,
            activity_cost
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
        return {"id": new_id, "message": "Ghi nhận hoạt động chăm sóc & ngoại giao thành công"}
    except Exception as e:
        conn.close()
        raise HTTPException(status_code=400, detail=str(e))
