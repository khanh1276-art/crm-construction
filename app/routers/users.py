"""
Users & Account Management Router for Executive Construction CRM.
Allows Admin (Ban Lãnh Đạo) to Create, Edit, and Delete accounts.
Provides role enforcement information for GĐKD SBU members.
"""
from fastapi import APIRouter, HTTPException, Header
from app.database import get_db
from app.schemas import UserCreate, UserUpdate, LoginRequest
from typing import Optional

router = APIRouter(prefix="/api/users", tags=["Users & Account Management"])

SBU_NAMES = {
    "ALL": "Toàn bộ Tập đoàn (5 Khối SBU)",
    "SBU1": "SBU 1 - Nền móng và Hầm",
    "SBU2": "SBU 2 - Xây dựng Năng lượng và công nghiệp",
    "SBU3": "SBU 3 - Metro và ngầm đô thị",
    "SBU4": "SBU 4 - Hạ tầng tập trung và đường sắt cao tốc",
    "SBU5": "SBU 5 - Cảng biển và biến đổi khí hậu"
}

@router.get("")
def list_users():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT id, username, full_name, role, sbu, title, email, phone, avatar_icon FROM users ORDER BY id ASC")
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()

    for r in rows:
        r["sbu_name"] = SBU_NAMES.get(r["sbu"], r["sbu"])
    return rows

@router.post("/login")
def login(data: LoginRequest):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT id, username, password, full_name, role, sbu, title, email, phone, avatar_icon FROM users WHERE username = ?", (data.username.strip(),))
    row = cursor.fetchone()
    conn.close()

    if not row:
        raise HTTPException(status_code=401, detail="Tài khoản không tồn tại trên hệ thống!")
    
    user = dict(row)
    # Check password (default is 123456)
    if user.get("password") and user.get("password") != data.password.strip():
        raise HTTPException(status_code=401, detail="Mật khẩu không chính xác! Vui lòng thử lại.")

    user["sbu_name"] = SBU_NAMES.get(user["sbu"], user["sbu"])
    return {
        "success": True,
        "message": f"Đăng nhập thành công! Chào mừng {user['full_name']}",
        "user": user
    }

@router.post("")
def create_user(
    data: UserCreate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    # Only Admin (Ban Lãnh Đạo) is permitted to create new accounts
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Chỉ có Ban Lãnh Đạo (Admin) mới có quyền tạo mới tài khoản người dùng!"
        )

    conn = get_db()
    cursor = conn.cursor()

    # Check username duplicate
    cursor.execute("SELECT id FROM users WHERE username = ?", (data.username.strip(),))
    if cursor.fetchone():
        conn.close()
        raise HTTPException(status_code=400, detail="Tên đăng nhập này đã tồn tại trong hệ thống!")

    try:
        cursor.execute("""
        INSERT INTO users (username, password, full_name, role, sbu, title, email, phone, avatar_icon)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            data.username.strip(), data.password or "123456", data.full_name, data.role, data.sbu,
            data.title, data.email, data.phone, data.avatar_icon or "fa-user-tie"
        ))
        new_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {"id": new_id, "message": "Tạo tài khoản thành công"}
    except Exception as e:
        conn.close()
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{user_id}")
def update_user(
    user_id: int,
    data: UserUpdate,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    # Admin can edit any account; SBU director cannot change roles/sbu of others
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Chỉ có Ban Lãnh Đạo (Admin) mới có quyền phân quyền và chỉnh sửa tài khoản!"
        )

    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM users WHERE id = ?", (user_id,))
    existing = cursor.fetchone()
    if not existing:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy tài khoản người dùng")

    cursor.execute("""
    UPDATE users SET
        full_name = COALESCE(?, full_name),
        password = CASE WHEN ? IS NOT NULL AND ? != '' THEN ? ELSE password END,
        role = COALESCE(?, role),
        sbu = COALESCE(?, sbu),
        title = COALESCE(?, title),
        email = COALESCE(?, email),
        phone = COALESCE(?, phone),
        avatar_icon = COALESCE(?, avatar_icon)
    WHERE id = ?
    """, (
        data.full_name,
        data.password, data.password, data.password,
        data.role, data.sbu, data.title,
        data.email, data.phone, data.avatar_icon, user_id
    ))
    conn.commit()
    conn.close()
    return {"message": "Cập nhật tài khoản người dùng thành công"}

@router.delete("/{user_id}")
def delete_user(
    user_id: int,
    x_user_role: Optional[str] = Header("ADMIN", alias="X-User-Role")
):
    # Only Admin is permitted to delete accounts
    if x_user_role != "ADMIN":
        raise HTTPException(
            status_code=403,
            detail="Chỉ có Ban Lãnh Đạo (Admin) mới có quyền xóa tài khoản người dùng!"
        )

    conn = get_db()
    cursor = conn.cursor()

    # Prevent deleting the root admin account
    cursor.execute("SELECT username FROM users WHERE id = ?", (user_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Không tìm thấy tài khoản")
    if row["username"] == "admin":
        conn.close()
        raise HTTPException(status_code=400, detail="Không thể xóa tài khoản Ban Lãnh Đạo mặc định!")

    cursor.execute("DELETE FROM users WHERE id = ?", (user_id,))
    conn.commit()
    conn.close()
    return {"message": "Đã xóa tài khoản người dùng"}

@router.get("/sbus")
def list_sbus():
    return [
        {"id": "SBU1", "code": "SBU1", "name": "Nền móng và Hầm", "icon": "fa-layer-group", "color": "blue"},
        {"id": "SBU2", "code": "SBU2", "name": "Xây dựng Năng lượng và công nghiệp", "icon": "fa-bolt", "color": "amber"},
        {"id": "SBU3", "code": "SBU3", "name": "Metro và ngầm đô thị", "icon": "fa-train-subway", "color": "purple"},
        {"id": "SBU4", "code": "SBU4", "name": "Hạ tầng tập trung và đường sắt cao tốc", "icon": "fa-road", "color": "emerald"},
        {"id": "SBU5", "code": "SBU5", "name": "Cảng biển và biến đổi khí hậu", "icon": "fa-anchor", "color": "cyan"}
    ]
