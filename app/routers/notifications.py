"""
Executive Notification & Zalo Automation Router for 5 SBUs.
"""
from fastapi import APIRouter, HTTPException, Query
from app.database import get_db
from app.schemas import SendExecutiveMessageRequest
from app.services.notification_service import TEMPLATES, send_executive_message, run_automated_executive_scan
from typing import Optional

router = APIRouter(prefix="/api/notifications", tags=["Executive Automation & Messages"])

@router.get("")
def list_notifications(
    sbu: Optional[str] = None,
    channel: Optional[str] = None,
    customer_id: Optional[int] = None,
    limit: int = 50
):
    conn = get_db()
    cursor = conn.cursor()

    query = """
    SELECT m.*, c.name as customer_name, c.key_decision_maker, p.name as project_name
    FROM automated_messages m
    JOIN customers c ON m.customer_id = c.id
    LEFT JOIN projects p ON m.project_id = p.id
    WHERE 1=1
    """
    params = []

    if sbu and sbu != "ALL":
        query += " AND m.sbu = ?"
        params.append(sbu)

    if channel:
        query += " AND m.channel = ?"
        params.append(channel)

    if customer_id:
        query += " AND m.customer_id = ?"
        params.append(customer_id)

    query += " ORDER BY m.id DESC LIMIT ?"
    params.append(limit)

    cursor.execute(query, params)
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

@router.get("/templates")
def get_templates():
    return TEMPLATES

@router.post("/send")
def send_message(data: SendExecutiveMessageRequest):
    try:
        res = send_executive_message(
            customer_id=data.customer_id,
            project_id=data.project_id,
            sbu=data.sbu,
            channel=data.channel,
            template_type=data.template_type,
            recipient=data.recipient,
            title=data.title,
            message_body=data.message_body
        )
        return res
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/run-auto")
def trigger_automated_scan():
    results = run_automated_executive_scan()
    return {
        "message": f"Đã quét và kích hoạt thành công {len(results)} tin nhắn ngoại giao tự động!",
        "count": len(results),
        "results": results
    }
