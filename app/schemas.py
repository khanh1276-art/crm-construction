"""
Pydantic Schemas for Executive Construction CRM (5 SBUs).
Includes Account Management & Strict Role-based validation.
"""
from pydantic import BaseModel
from typing import Optional, List, Any, Dict

# --- User & Account Management Schemas ---
class LoginRequest(BaseModel):
    username: str
    password: str

class UserCreate(BaseModel):
    username: str
    password: Optional[str] = "123456"
    full_name: str
    role: str = "SBU_DIRECTOR"  # ADMIN (Ban Lãnh Đạo), SBU_DIRECTOR (GĐKD SBU), COLLABORATOR (Cộng Tác Viên)
    sbu: str = "SBU1"           # ALL (for Admin & CTV toàn quốc), SBU1, SBU2, SBU3, SBU4, SBU5
    title: str
    email: Optional[str] = None
    phone: Optional[str] = None
    avatar_icon: Optional[str] = "fa-user-tie"

class UserUpdate(BaseModel):
    password: Optional[str] = None
    full_name: Optional[str] = None
    role: Optional[str] = None
    sbu: Optional[str] = None
    title: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    avatar_icon: Optional[str] = None
    status: Optional[str] = "ACTIVE"

class UserResponse(BaseModel):
    id: int
    username: str
    full_name: str
    role: str
    sbu: str
    title: str
    email: Optional[str] = None
    phone: Optional[str] = None
    avatar_icon: Optional[str] = None

# --- Customer Schemas ---
class CustomerCreate(BaseModel):
    code: Optional[str] = None
    name: str
    sbu: str  # SBU1 to SBU5
    tier: str = "STRATEGIC_VIP"  # STRATEGIC_VIP, CLOSE_PARTNER, PROSPECT
    segment: str = "B2B"  # B2B, B2G, FDI
    tax_code: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    headquarters: Optional[str] = None
    key_decision_maker: str
    decision_maker_role: Optional[str] = None
    decision_maker_phone: Optional[str] = None
    decision_maker_birthday: Optional[str] = None
    founding_anniversary: Optional[str] = None
    relationship_score: int = 5
    relationship_status: str = "EXCELLENT"
    strategic_notes: Optional[str] = None
    reused_from_id: Optional[int] = None # If reused from existing customer

class CustomerUpdate(CustomerCreate):
    pass

# --- Pipeline Bid Schemas ---
class BidCreate(BaseModel):
    customer_id: int
    sbu: str
    project_title: str
    estimated_value: float = 0.0
    stage: str = "INFORMATION"
    win_rate: int = 50
    tender_deadline: Optional[str] = None
    target_kickoff: Optional[str] = None
    assigned_director: Optional[str] = None
    bidding_notes: Optional[str] = None

class BidStageUpdate(BaseModel):
    stage: str
    win_rate: Optional[int] = None
    bidding_notes: Optional[str] = None

# --- Project Schemas ---
class ProjectCreate(BaseModel):
    code: str
    name: str
    sbu: str
    customer_id: int
    contract_number: Optional[str] = None
    contract_value: float = 0.0
    start_date: Optional[str] = None
    expected_end_date: Optional[str] = None
    project_director: Optional[str] = None
    summary_scope: Optional[str] = None
    project_health: str = "GOOD"

class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    progress_percent: Optional[float] = None
    paid_amount: Optional[float] = None
    project_health: Optional[str] = None
    status: Optional[str] = None
    summary_scope: Optional[str] = None

# --- Milestone Schemas ---
class MilestoneCreate(BaseModel):
    project_id: int
    title: str
    due_date: Optional[str] = None
    percentage: float = 0.0
    amount: float = 0.0
    payment_status: str = "PENDING"
    notes: Optional[str] = None

class MilestoneUpdate(BaseModel):
    payment_status: Optional[str] = None
    due_date: Optional[str] = None
    notes: Optional[str] = None

# --- Customer Care Activity ---
class CareActivityCreate(BaseModel):
    customer_id: int
    sbu: str
    activity_type: str = "EXECUTIVE_MEETING"
    title: str
    content: Optional[str] = None
    occurred_at: str
    leader_in_charge: str
    outcome_status: str = "SUCCESS"

# --- Executive Message ---
class SendExecutiveMessageRequest(BaseModel):
    customer_id: int
    project_id: Optional[int] = None
    sbu: str
    channel: str = "ZALO_ZNS"
    template_type: str = "CHUC_MUNG_SINH_NHAT"
    recipient: str
    title: str
    message_body: str
