"""
Main FastAPI Application for Executive Construction CRM (5 SBUs).
Focused purely on:
- Customer Relationship Management (CRM) & Strategic Accounts
- Bidding & Opportunity Pipeline
- Executive Project & Cashflow Tracking
- High-level Customer Care & Automated Executive Messaging (Zalo/SMS/Email)
- Role-based Access for Board of Directors (Admin) and 5 SBU Directors
"""
from pathlib import Path
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

from app.database import init_db
from app.routers import dashboard, customers, projects, care_activities, notifications, users

# Initialize DB
init_db()

app = FastAPI(
    title="Executive Construction Enterprise CRM",
    description="Hệ thống Quản trị & Chăm sóc Khách hàng Chiến lược dành cho Ban Lãnh Đạo & 5 Khối SBU Kinh Doanh",
    version="2.0.0"
)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(users.router)
app.include_router(dashboard.router)
app.include_router(customers.router)
app.include_router(projects.router)
app.include_router(care_activities.router)
app.include_router(notifications.router)

# Mount Static Files
STATIC_DIR = Path(__file__).resolve().parent / "static"
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

@app.get("/")
def get_executive_portal():
    return FileResponse(str(STATIC_DIR / "index.html"))

@app.get("/api/health")
def health_check():
    return {
        "status": "healthy",
        "app": "Executive Construction CRM",
        "sbus": [
            "SBU1 - Nền móng và Hầm",
            "SBU2 - Xây dựng Năng lượng và công nghiệp",
            "SBU3 - Metro và ngầm đô thị",
            "SBU4 - Hạ tầng tập trung và đường sắt cao tốc",
            "SBU5 - Cảng biển và biến đổi khí hậu"
        ]
    }
