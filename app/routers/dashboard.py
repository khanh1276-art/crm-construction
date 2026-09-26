"""
Executive Dashboard Analytics for Construction Enterprise CRM.
Supports full corporate visibility for Board of Directors (Admin)
and dedicated views for the 5 SBU Business Development Directors.
"""
from fastapi import APIRouter, Query
from app.database import get_db
from typing import Optional
from datetime import datetime

router = APIRouter(prefix="/api/dashboard", tags=["Executive Dashboard"])

SBU_META = {
    "SBU1": {"name": "Nền móng và Hầm", "icon": "fa-layer-group", "color": "#3b82f6"},
    "SBU2": {"name": "Xây dựng Năng lượng và công nghiệp", "icon": "fa-bolt", "color": "#f59e0b"},
    "SBU3": {"name": "Metro và ngầm đô thị", "icon": "fa-train-subway", "color": "#a855f7"},
    "SBU4": {"name": "Hạ tầng tập trung và đường sắt cao tốc", "icon": "fa-road", "color": "#10b981"},
    "SBU5": {"name": "Cảng biển và biến đổi khí hậu", "icon": "fa-anchor", "color": "#06b6d4"}
}

@router.get("/metrics")
def get_executive_metrics(sbu: Optional[str] = Query(None)):
    conn = get_db()
    cursor = conn.cursor()

    is_all = not sbu or sbu == "ALL"

    # 1. Total Customers
    cust_query = "SELECT COUNT(*) FROM customers" + ("" if is_all else " WHERE sbu = ?")
    cust_params = [] if is_all else [sbu]
    cursor.execute(cust_query, cust_params)
    total_customers = cursor.fetchone()[0]

    # 2. Projects & Contract Values
    proj_query = "SELECT COUNT(*), COALESCE(SUM(contract_value), 0), COALESCE(SUM(paid_amount), 0) FROM projects" + ("" if is_all else " WHERE sbu = ?")
    proj_params = [] if is_all else [sbu]
    cursor.execute(proj_query, proj_params)
    proj_row = cursor.fetchone()
    total_projects = proj_row[0]
    total_contract_value = proj_row[1]
    total_paid_amount = proj_row[2]
    unpaid_balance = total_contract_value - total_paid_amount

    # 3. Pipeline Bids
    bids_query = "SELECT COUNT(*), COALESCE(SUM(estimated_value), 0) FROM pipeline_bids WHERE stage NOT IN ('WON', 'LOST')" + ("" if is_all else " AND sbu = ?")
    bids_params = [] if is_all else [sbu]
    cursor.execute(bids_query, bids_params)
    bids_row = cursor.fetchone()
    active_bids_count = bids_row[0]
    active_bids_value = bids_row[1]

    # 4. SBU Comparison Matrix (Always computed for corporate context)
    cursor.execute("""
    SELECT sbu, COUNT(*) as proj_count, COALESCE(SUM(contract_value), 0) as contract_val,
           COALESCE(SUM(paid_amount), 0) as paid_val
    FROM projects
    GROUP BY sbu
    """)
    sbu_projects = {r["sbu"]: r for r in cursor.fetchall()}

    cursor.execute("""
    SELECT sbu, COUNT(*) as bid_count, COALESCE(SUM(estimated_value), 0) as bid_val
    FROM pipeline_bids
    WHERE stage NOT IN ('WON', 'LOST')
    GROUP BY sbu
    """)
    sbu_bids = {r["sbu"]: r for r in cursor.fetchall()}

    cursor.execute("SELECT sbu, COUNT(*) as cust_count FROM customers GROUP BY sbu")
    sbu_custs = {r["sbu"]: r["cust_count"] for r in cursor.fetchall()}

    sbu_matrix = []
    for s_code, meta in SBU_META.items():
        p_info = sbu_projects.get(s_code, {"proj_count": 0, "contract_val": 0, "paid_val": 0})
        b_info = sbu_bids.get(s_code, {"bid_count": 0, "bid_val": 0})
        sbu_matrix.append({
            "sbu": s_code,
            "name": meta["name"],
            "icon": meta["icon"],
            "color": meta["color"],
            "customer_count": sbu_custs.get(s_code, 0),
            "project_count": p_info["proj_count"],
            "contract_value": p_info["contract_val"],
            "paid_amount": p_info["paid_val"],
            "balance": p_info["contract_val"] - p_info["paid_val"],
            "bid_count": b_info["bid_count"],
            "bid_value": b_info["bid_val"]
        })

    # 5. Critical Cashflow Milestones (Mốc nghiệm thu & giải ngân lớn)
    m_query = """
    SELECT m.*, p.name as project_name, p.code as project_code, p.sbu, c.name as customer_name, c.decision_maker_phone
    FROM milestones m
    JOIN projects p ON m.project_id = p.id
    JOIN customers c ON p.customer_id = c.id
    WHERE m.payment_status IN ('PENDING', 'INVOICED')
    """
    m_params = []
    if not is_all:
        m_query += " AND p.sbu = ?"
        m_params.append(sbu)
    m_query += " ORDER BY m.due_date ASC LIMIT 6"
    cursor.execute(m_query, m_params)
    upcoming_payments = [dict(r) for r in cursor.fetchall()]

    # 6. Upcoming VIP Birthdays & Founding Anniversaries (Chăm sóc ngoại giao)
    today = datetime.now()
    b_query = "SELECT id, code, name, sbu, key_decision_maker, decision_maker_role, decision_maker_phone, decision_maker_birthday, founding_anniversary FROM customers WHERE 1=1"
    b_params = []
    if not is_all:
        b_query += " AND sbu = ?"
        b_params.append(sbu)
    cursor.execute(b_query, b_params)

    reminders = []
    for row in cursor.fetchall():
        bday_str = row["decision_maker_birthday"]
        if bday_str:
            try:
                bday = datetime.strptime(bday_str, "%Y-%m-%d")
                this_year_bday = bday.replace(year=today.year)
                if this_year_bday.date() < today.date():
                    this_year_bday = bday.replace(year=today.year + 1)
                days_diff = (this_year_bday.date() - today.date()).days
                if days_diff <= 60:
                    item = dict(row)
                    item["reminder_type"] = "BIRTHDAY"
                    item["target_date"] = this_year_bday.strftime("%d/%m")
                    item["days_until"] = days_diff
                    item["event_title"] = f"Sinh nhật {row['key_decision_maker']}"
                    reminders.append(item)
            except Exception:
                pass

        anniv_str = row["founding_anniversary"]
        if anniv_str:
            try:
                anniv = datetime.strptime(anniv_str, "%Y-%m-%d")
                this_year_anniv = anniv.replace(year=today.year)
                if this_year_anniv.date() < today.date():
                    this_year_anniv = anniv.replace(year=today.year + 1)
                days_diff = (this_year_anniv.date() - today.date()).days
                if days_diff <= 60:
                    item = dict(row)
                    item["reminder_type"] = "ANNIVERSARY"
                    item["target_date"] = this_year_anniv.strftime("%d/%m")
                    item["days_until"] = days_diff
                    item["event_title"] = f"Ngày thành lập {row['name']}"
                    reminders.append(item)
            except Exception:
                pass

    reminders.sort(key=lambda x: x["days_until"])

    # 7. Recent High-Level Care Activities
    act_query = """
    SELECT a.*, c.name as customer_name, c.key_decision_maker
    FROM customer_care_activities a
    JOIN customers c ON a.customer_id = c.id
    WHERE 1=1
    """
    act_params = []
    if not is_all:
        act_query += " AND a.sbu = ?"
        act_params.append(sbu)
    act_query += " ORDER BY a.occurred_at DESC, a.id DESC LIMIT 5"
    cursor.execute(act_query, act_params)
    # 8. Customer Tiers Summary & Care Budget
    tier_query = "SELECT tier, COUNT(*), COALESCE(SUM(annual_care_budget), 0), COALESCE(SUM(spent_care_budget), 0) FROM customers" + ("" if is_all else " WHERE sbu = ?") + " GROUP BY tier"
    cursor.execute(tier_query, [] if is_all else [sbu])
    diamond_count = 0
    gold_count = 0
    silver_count = 0
    total_care_budget = 0.0
    total_care_spent = 0.0

    for r in cursor.fetchall():
        t = r[0]
        cnt = r[1]
        budget = r[2]
        spent = r[3]
        total_care_budget += budget
        total_care_spent += spent
        if t in ('DIAMOND', 'STRATEGIC_VIP'):
            diamond_count += cnt
        elif t in ('GOLD', 'CLOSE_PARTNER'):
            gold_count += cnt
        elif t in ('SILVER', 'PROSPECT'):
            silver_count += cnt

    conn.close()

    return {
        "overview": {
            "total_customers": total_customers,
            "diamond_count": diamond_count,
            "gold_count": gold_count,
            "silver_count": silver_count,
            "total_care_budget": total_care_budget,
            "total_care_spent": total_care_spent,
            "total_projects": total_projects,
            "total_contract_value": total_contract_value,
            "total_paid_amount": total_paid_amount,
            "unpaid_balance": unpaid_balance,
            "active_bids_count": active_bids_count,
            "active_bids_value": active_bids_value,
            "sbu": sbu or "ALL"
        },
        "sbu_matrix": sbu_matrix,
        "upcoming_payments": upcoming_payments,
        "executive_reminders": reminders[:6],
        "recent_activities": recent_activities
    }
