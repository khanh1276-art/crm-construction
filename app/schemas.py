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
    tier: str = "GOLD"  # DIAMOND (Kim Cương), GOLD (Vàng), SILVER (Bạc)
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
    # FECON 5 Criteria Assessment & Budget Limits
    score_scale_project: Optional[float] = 15.0
    score_fecon_fit: Optional[float] = 25.0
    score_financial_capacity: Optional[float] = 25.0
    score_cooperation_history: Optional[float] = 20.0
    score_management_capacity: Optional[float] = 15.0
    total_score: Optional[float] = 100.0
    is_special_elevated: Optional[int] = 0
    veto_applied: Optional[int] = 0
    annual_care_budget: Optional[float] = 80000000.0
    spent_care_budget: Optional[float] = 0.0
    in_charge_executive: Optional[str] = 'Chủ tịch / TGĐ trực tiếp phụ trách'
    care_frequency: Optional[str] = '1 tháng / lần'

class CustomerUpdate(CustomerCreate):
    pass

class CustomerAssessRequest(BaseModel):
    score_scale_project: float # Max 15
    score_fecon_fit: float # Max 25
    score_financial_capacity: float # Max 25 (TIÊU CHÍ PHỦ QUYẾT: nếu 0 -> tối đa Hạng Vàng)
    score_cooperation_history: float # Max 20
    score_management_capacity: float # Max 15
    is_special_elevated: bool = False # CT HĐQT / TGĐ đặc cách lên Kim Cương
    strategic_notes: Optional[str] = None

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
    project_level: Optional[str] = None
    project_level_name: Optional[str] = None
    approver_authority: Optional[str] = None

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
    project_level: Optional[str] = None
    project_level_name: Optional[str] = None
    approver_authority: Optional[str] = None

class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    progress_percent: Optional[float] = None
    paid_amount: Optional[float] = None
    contract_value: Optional[float] = None
    project_health: Optional[str] = None
    status: Optional[str] = None
    summary_scope: Optional[str] = None
    project_level: Optional[str] = None
    project_level_name: Optional[str] = None
    approver_authority: Optional[str] = None

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
    project_id: Optional[int] = None # Dự án liên kết để xác định thẩm quyền duyệt
    sbu: str
    activity_type: str = "EXECUTIVE_MEETING"
    title: str
    content: Optional[str] = None
    occurred_at: str
    leader_in_charge: str
    outcome_status: str = "SUCCESS"
    cost: Optional[float] = 0.0 # Chi phí tiếp khách / quà tặng phát sinh thực tế
    project_level: Optional[str] = None
    approver_authority: Optional[str] = None
    approval_status: Optional[str] = "APPROVED"

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
