/**
 * EXECUTIVE CONSTRUCT-CRM Frontend Logic
 * Supports:
 * 1. Account Management (Admin creates, edits, deletes; SBU Directors create & edit)
 * 2. Strict SBU Scope Enforcement (GĐKD SBU only manages their SBU)
 * 3. Real-time Customer Duplicate Detection & Immediate Data Reuse
 */

let allUsers = [];
let currentUser = null;
let currentSBU = "ALL";
let currentTab = "dashboard";

let allCustomers = [];
let allProjects = [];
let lastMatchedCustomer = null;

const SBU_CONFIG = {
  "ALL": { name: "Toàn Tập Đoàn (5 SBU)", icon: "fa-globe", color: "amber", badgeBg: "bg-slate-800 text-white" },
  "SBU1": { name: "SBU 1 - Nền móng và Hầm", icon: "fa-layer-group", color: "blue", badgeBg: "bg-blue-100 text-blue-800 border-blue-300" },
  "SBU2": { name: "SBU 2 - Năng lượng & Công nghiệp", icon: "fa-bolt", color: "amber", badgeBg: "bg-amber-100 text-amber-800 border-amber-300" },
  "SBU3": { name: "SBU 3 - Metro & Ngầm đô thị", icon: "fa-train-subway", color: "purple", badgeBg: "bg-purple-100 text-purple-800 border-purple-300" },
  "SBU4": { name: "SBU 4 - Hạ tầng & ĐS cao tốc", icon: "fa-road", color: "emerald", badgeBg: "bg-emerald-100 text-emerald-800 border-emerald-300" },
  "SBU5": { name: "SBU 5 - Cảng biển & BĐKH", icon: "fa-anchor", color: "cyan", badgeBg: "bg-cyan-100 text-cyan-800 border-cyan-300" }
};

document.addEventListener("DOMContentLoaded", async () => {
  await checkAuthAndInitialize();
  checkIosDeviceAndShowBanner();
});

// Helper: Custom fetch with Role & SBU headers
async function authFetch(url, options = {}) {
  if (!currentUser) {
    showLoginScreen();
    throw new Error("Chưa đăng nhập");
  }

  const headers = options.headers || {};
  headers['X-User-Role'] = currentUser.role;
  headers['X-User-SBU'] = currentUser.sbu;
  options.headers = headers;

  const res = await fetch(url, options);
  if (res.status === 401) {
    showToast("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!", "error");
    handleLogout();
    throw new Error("Unauthorized");
  }
  return res;
}

// Currency format helper
function formatVND(amount) {
  if (!amount || amount === 0) return "0 đ";
  if (amount >= 1000000000) {
    const val = (amount / 1000000000).toFixed(1);
    return `${val.endsWith('.0') ? val.slice(0, -2) : val} Tỷ`;
  }
  if (amount >= 1000000) {
    return `${(amount / 1000000).toFixed(0)} Tr`;
  }
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function getSBUBadge(sbu) {
  const cfg = SBU_CONFIG[sbu] || { name: sbu, badgeBg: "bg-slate-100 text-slate-800", icon: "fa-building" };
  return `<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold border ${cfg.badgeBg}"><i class="fa-solid ${cfg.icon}"></i> ${cfg.name}</span>`;
}

function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  const bg = type === 'success' ? 'bg-emerald-600 text-white' : type === 'error' ? 'bg-rose-600 text-white' : 'bg-slate-900 text-white';
  const icon = type === 'success' ? 'fa-circle-check' : type === 'error' ? 'fa-triangle-exclamation' : 'fa-bell';

  toast.className = `p-3.5 rounded-xl shadow-xl flex items-center gap-2.5 text-xs font-semibold ${bg} transition-all transform duration-300 translate-y-2 pointer-events-auto`;
  toast.innerHTML = `<i class="fa-solid ${icon} text-sm"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-4');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('hidden');
}
function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('hidden');
}

// ==========================================
// 1. MANDATORY AUTHENTICATION & LOGIN LOGIC
// ==========================================
async function checkAuthAndInitialize() {
  const authData = localStorage.getItem('crm_auth_user');
  if (!authData) {
    showLoginScreen();
    return;
  }

  try {
    currentUser = JSON.parse(authData);
    await loadUsers();
    // Verify currentUser is still present in database
    const matched = allUsers.find(u => u.username === currentUser.username);
    if (matched) {
      currentUser = matched;
      localStorage.setItem('crm_auth_user', JSON.stringify(currentUser));
      showAppScreen();
      applyUserRoleState();
      refreshAllData();
    } else {
      showLoginScreen();
    }
  } catch (err) {
    console.error("Auth init error:", err);
    showLoginScreen();
  }
}

function showLoginScreen() {
  const screen = document.getElementById('login-screen');
  const app = document.getElementById('app-root');
  if (screen) screen.classList.remove('hidden');
  if (app) app.classList.add('hidden');
}

function showAppScreen() {
  const screen = document.getElementById('login-screen');
  const app = document.getElementById('app-root');
  if (screen) screen.classList.add('hidden');
  if (app) app.classList.remove('hidden');
}

async function handleScreenLogin(e) {
  e.preventDefault();
  const usernameInput = document.getElementById('screen-login-username');
  const passwordInput = document.getElementById('screen-login-password');
  const submitBtn = document.getElementById('btn-submit-login');

  const username = usernameInput.value.trim();
  const password = passwordInput.value.trim();

  if (!username || !password) {
    showToast("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", "error");
    return;
  }

  const originalBtnText = submitBtn.innerHTML;
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xác thực...';

  try {
    const res = await fetch('/api/users/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (res.ok) {
      const data = await res.json();
      currentUser = data.user;
      localStorage.setItem('crm_auth_user', JSON.stringify(currentUser));
      
      showAppScreen();
      await loadUsers();
      applyUserRoleState();
      refreshAllData();
      showToast(data.message || `Đăng nhập thành công với vai trò: ${currentUser.full_name}`);
    } else {
      const err = await res.json();
      showToast(err.detail || "Tên đăng nhập hoặc mật khẩu không chính xác!", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối máy chủ xác thực!", "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerHTML = originalBtnText;
  }
}

function handleLogout() {
  localStorage.removeItem('crm_auth_user');
  currentUser = null;
  const userEl = document.getElementById('screen-login-username');
  const passEl = document.getElementById('screen-login-password');
  if (userEl) userEl.value = '';
  if (passEl) passEl.value = '';
  showLoginScreen();
  showToast("Đã đăng xuất khỏi hệ thống!");
}

function togglePasswordVisibility(inputId) {
  const input = document.getElementById(inputId);
  const icon = document.getElementById(`eye-icon-${inputId}`);
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    if (icon) icon.className = 'fa-solid fa-eye-slash';
  } else {
    input.type = 'password';
    if (icon) icon.className = 'fa-solid fa-eye';
  }
}

async function loadUsers() {
  try {
    const res = await authFetch('/api/users');
    allUsers = await res.json();

    if (currentTab === 'users') {
      renderUsersTable();
    }
  } catch (err) {
    console.error("Error loading users:", err);
  }
}

function applyUserRoleState() {
  if (!currentUser) return;

  // 1. Header user widget
  const headerName = document.getElementById('user-header-name');
  const headerBadge = document.getElementById('user-header-role-badge');
  const headerTitle = document.getElementById('user-header-title');
  const iconEl = document.getElementById('user-avatar-icon');

  if (headerName) headerName.innerText = currentUser.full_name;
  if (headerTitle) headerTitle.innerText = currentUser.title;

  if (iconEl) {
    let iconBg = 'bg-amber-400/20 text-amber-400';
    if (currentUser.role === 'SBU_DIRECTOR') iconBg = 'bg-blue-500/20 text-blue-400';
    if (currentUser.role === 'COLLABORATOR') iconBg = 'bg-emerald-500/20 text-emerald-400';
    iconEl.className = `w-8 h-8 rounded-lg ${iconBg} flex items-center justify-center text-sm group-hover:scale-105 transition`;
    iconEl.innerHTML = `<i class="fa-solid ${currentUser.avatar_icon || 'fa-user-tie'}"></i>`;
  }

  if (headerBadge) {
    if (currentUser.role === 'ADMIN') {
      headerBadge.className = 'px-1.5 py-0.5 bg-amber-400/20 text-amber-300 text-[9px] font-black rounded uppercase';
      headerBadge.innerText = 'ADMIN';
    } else if (currentUser.role === 'COLLABORATOR') {
      headerBadge.className = 'px-1.5 py-0.5 bg-emerald-400/20 text-emerald-300 text-[9px] font-black rounded uppercase';
      headerBadge.innerText = 'CTV';
    } else {
      headerBadge.className = 'px-1.5 py-0.5 bg-blue-400/20 text-blue-300 text-[9px] font-black rounded uppercase';
      headerBadge.innerText = currentUser.sbu;
    }
  }

  const pillsBar = document.getElementById('sbu-pills-bar');
  const mobSbuBar = document.getElementById('mobile-sbu-bar');
  const bannerBadge = document.getElementById('banner-role-badge');
  const dashTitle = document.getElementById('dashboard-title');
  const dashSub = document.getElementById('dashboard-subtitle');
  const sbuBadge = document.getElementById('sidebar-sbu-name');
  const navUsers = document.getElementById('nav-users');
  const mobNavUsers = document.getElementById('mob-nav-users');
  const mobBottomNav = document.getElementById('mobile-bottom-nav-grid');

  if (currentUser.role === 'ADMIN') {
    // Leadership (Admin)
    if (pillsBar) pillsBar.classList.remove('hidden');
    if (mobSbuBar) mobSbuBar.classList.remove('hidden');
    if (navUsers) navUsers.classList.remove('hidden');
    if (mobNavUsers) mobNavUsers.classList.remove('hidden');
    if (mobBottomNav) mobBottomNav.className = 'grid grid-cols-5 h-16 items-center px-1 text-center';
    currentSBU = "ALL";
    if (bannerBadge) bannerBadge.innerText = "Cổng Điều Hành Ban Lãnh Đạo (Admin)";
    if (dashTitle) dashTitle.innerText = "Báo Cáo Điều Hành Khách Hàng & Kinh Doanh";
    if (dashSub) dashSub.innerText = "Toàn quyền quản trị, thêm mới, sửa, xóa đối tác và hồ sơ dự thầu trên cả 5 Khối SBU.";
    if (sbuBadge) sbuBadge.innerHTML = `<i class="fa-solid fa-globe text-amber-500"></i> Toàn Tập Đoàn (5 SBU)`;
  } else if (currentUser.role === 'COLLABORATOR') {
    // Collaborator (CTV)
    currentSBU = currentUser.sbu;
    if (pillsBar) pillsBar.classList.toggle('hidden', currentUser.sbu !== 'ALL');
    if (mobSbuBar) mobSbuBar.classList.toggle('hidden', currentUser.sbu !== 'ALL');
    if (navUsers) navUsers.classList.add('hidden');
    if (mobNavUsers) mobNavUsers.classList.add('hidden');
    if (mobBottomNav) mobBottomNav.className = 'grid grid-cols-4 h-16 items-center px-1 text-center';
    if (currentTab === 'users') switchTab('dashboard');

    if (bannerBadge) bannerBadge.innerText = "Cổng Kết Nối Dành Cho Cộng Tác Viên (CTV)";
    if (dashTitle) dashTitle.innerText = "Mạng Lưới Phát Triển Khách Hàng & Cơ Hội Dự Án";
    if (dashSub) dashSub.innerText = "Quyền hạn: Giới thiệu thông tin đối tác tiềm năng & hồ sơ dự thầu; Theo dõi tiến độ chung.";
    if (sbuBadge) sbuBadge.innerHTML = `<i class="fa-solid fa-handshake text-emerald-500"></i> Mạng Lưới CTV (${currentUser.sbu === 'ALL' ? 'Toàn Quốc' : currentUser.sbu})`;
  } else {
    // SBU Director (Member)
    currentSBU = currentUser.sbu;
    if (pillsBar) pillsBar.classList.add('hidden');
    if (mobSbuBar) mobSbuBar.classList.add('hidden');
    if (navUsers) navUsers.classList.add('hidden');
    if (mobNavUsers) mobNavUsers.classList.add('hidden');
    if (mobBottomNav) mobBottomNav.className = 'grid grid-cols-4 h-16 items-center px-1 text-center';
    if (currentTab === 'users') switchTab('dashboard');

    const sbuCfg = SBU_CONFIG[currentSBU] || { name: currentSBU };
    if (bannerBadge) bannerBadge.innerText = `Cổng Điều Hành Giám Đốc Kinh Doanh`;
    if (dashTitle) dashTitle.innerText = `Báo Cáo Khách Hàng & Kinh Doanh: ${sbuCfg.name}`;
    if (dashSub) dashSub.innerText = `Quyền hạn: Thêm mới và cập nhật đối tác, gói thầu thuộc ${sbuCfg.name} (Không được xóa).`;
    if (sbuBadge) sbuBadge.innerHTML = `<i class="fa-solid ${sbuCfg.icon || 'fa-briefcase'} text-amber-500"></i> ${sbuCfg.name}`;
  }

  // Adjust button visibility and text based on role
  const btnAddProj = document.getElementById('btn-add-project');
  const labelAddCust = document.getElementById('label-add-customer');
  if (btnAddProj) {
    btnAddProj.classList.add('hidden');
  }
  if (labelAddCust) {
    labelAddCust.innerText = (currentUser.role === 'COLLABORATOR') ? 'Giới Thiệu Đối Tác' : 'Thêm Đối Tác';
  }

  updatePillStyles();
}

function changeSBUFilter(sbu) {
  if (currentUser.role !== 'ADMIN' && sbu !== currentUser.sbu && sbu !== 'ALL') {
    showToast("Bạn chỉ có quyền xem dữ liệu thuộc SBU của mình!", "error");
    return;
  }
  currentSBU = sbu;
  updatePillStyles();

  const sbuCfg = SBU_CONFIG[currentSBU] || { name: currentSBU };
  const sbuBadge = document.getElementById('sidebar-sbu-name');
  if (sbuBadge) {
    sbuBadge.innerHTML = `<i class="fa-solid ${sbuCfg.icon || 'fa-globe'} text-amber-500"></i> ${sbuCfg.name}`;
  }

  refreshAllData();
}

function updatePillStyles() {
  document.querySelectorAll('.sbu-filter-pill').forEach(btn => {
    btn.className = 'sbu-filter-pill px-2.5 py-1.5 rounded-lg text-slate-300 hover:text-white transition';
  });
  const activeBtn = document.getElementById(`pill-${currentSBU}`);
  if (activeBtn) activeBtn.className = 'sbu-filter-pill px-2.5 py-1.5 rounded-lg transition bg-amber-400 text-slate-950 font-bold';

  // Update mobile SBU scrollbar pills
  document.querySelectorAll('.mob-sbu-pill').forEach(btn => {
    btn.className = 'mob-sbu-pill px-2.5 py-1 rounded-lg shrink-0 text-slate-300 bg-slate-800 border border-slate-700 transition';
  });
  const activeMobBtn = document.getElementById(`mob-pill-${currentSBU}`);
  if (activeMobBtn) activeMobBtn.className = 'mob-sbu-pill px-2.5 py-1 rounded-lg shrink-0 transition bg-amber-400 text-slate-950 font-bold shadow-sm';
}

function refreshAllData() {
  loadDashboard();
  loadCustomers();
  loadPipeline();
  loadCareActivities();
  populateCustomerSelects();
  if (currentTab === 'users') renderUsersTable();
}

// Tab Switching
function switchTab(tabName) {
  if (tabName === 'projects') {
    tabName = 'pipeline';
  }

  if (tabName === 'users' && currentUser.role !== 'ADMIN') {
    showToast("Chỉ Ban Lãnh Đạo (Admin) mới có quyền truy cập Quản lý Tài khoản!", "error");
    return;
  }

  currentTab = tabName;
  const tabs = ['dashboard', 'customers', 'pipeline', 'care', 'users'];
  
  tabs.forEach(t => {
    const el = document.getElementById(`tab-${t}`);
    const nav = document.getElementById(`nav-${t}`);
    const mobNav = document.getElementById(`mob-nav-${t}`);
    if (el) el.classList.toggle('hidden', t !== tabName);
    if (nav) {
      if (t === tabName) {
        nav.className = 'nav-link w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-bold text-xs text-amber-900 bg-amber-50 transition';
      } else {
        nav.className = 'nav-link w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-medium text-xs text-slate-600 hover:bg-slate-50 transition';
      }
    }
    if (mobNav) {
      if (t === tabName) {
        mobNav.className = 'mobile-nav-btn flex flex-col items-center justify-center py-1 text-[#EA5713] font-black transition';
      } else {
        mobNav.className = 'mobile-nav-btn flex flex-col items-center justify-center py-1 text-slate-400 hover:text-slate-600 font-medium transition';
      }
    }
  });

  // Scroll to top on tab switch
  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (tabName === 'dashboard') loadDashboard();
  if (tabName === 'customers') loadCustomers();
  if (tabName === 'pipeline') loadPipeline();
  if (tabName === 'care') {
    populateCustomerSelects();
    loadCareActivities();
  }
  if (tabName === 'users') renderUsersTable();
}

function navigateToBidsPipeline(filterStage) {
  switchTab('pipeline');
  setTimeout(() => {
    if (filterStage === 'WON') {
      const wonCol = document.getElementById('kanban-col-WON');
      if (wonCol) {
        wonCol.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
        wonCol.classList.add('ring-4', 'ring-emerald-500', 'shadow-xl');
        setTimeout(() => wonCol.classList.remove('ring-4', 'ring-emerald-500', 'shadow-xl'), 2500);
      }
    } else {
      const board = document.getElementById('pipeline-board');
      if (board) {
        board.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  }, 300);
}

// User CRUD (Admin Only)
function renderUsersTable() {
  const tbody = document.getElementById('users-table-body');
  if (!tbody) return;

  document.getElementById('stat-user-count').innerText = allUsers.length;

  tbody.innerHTML = allUsers.map(u => {
    let roleBadge = '';
    if (u.role === 'ADMIN') {
      roleBadge = '<span class="px-2 py-0.5 rounded-full text-[10px] font-black bg-amber-100 text-amber-900 border border-amber-300">👑 Ban Lãnh Đạo (Admin)</span>';
    } else if (u.role === 'COLLABORATOR') {
      roleBadge = '<span class="px-2 py-0.5 rounded-full text-[10px] font-black bg-emerald-100 text-emerald-900 border border-emerald-300">🤝 Cộng Tác Viên (CTV)</span>';
    } else {
      roleBadge = '<span class="px-2 py-0.5 rounded-full text-[10px] font-black bg-blue-100 text-blue-900 border border-blue-300">💼 Giám Đốc KD (SBU)</span>';
    }

    return `
      <tr class="hover:bg-slate-50 transition">
        <td class="p-3.5">
          <div class="font-black text-slate-900 flex items-center gap-2">
            <i class="fa-solid ${u.avatar_icon || 'fa-user-tie'} text-slate-500"></i>
            <span>${u.full_name}</span>
          </div>
        </td>
        <td class="p-3.5">
          ${roleBadge}
        </td>
        <td class="p-3.5">
          ${u.sbu === 'ALL' ? '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold border bg-slate-800 text-white"><i class="fa-solid fa-globe"></i> Toàn Tập Đoàn (5 SBU)</span>' : getSBUBadge(u.sbu)}
        </td>
        <td class="p-3.5 text-slate-700 font-semibold text-xs">
          ${u.title}
        </td>
        <td class="p-3.5 text-[11px] text-slate-600">
          <div><i class="fa-solid fa-envelope text-slate-400 mr-1"></i> ${u.email || 'N/A'}</div>
          <div><i class="fa-solid fa-phone text-blue-500 mr-1"></i> ${u.phone || 'N/A'}</div>
        </td>
        <td class="p-3.5 text-right space-x-1">
          <button onclick="openEditUserModal(${u.id})" class="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-bold transition">
            <i class="fa-solid fa-pen-to-square"></i> Sửa
          </button>
          ${u.username !== 'admin' ? `
            <button onclick="deleteUserAccount(${u.id})" class="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-bold transition">
              <i class="fa-solid fa-trash"></i> Xóa
            </button>
          ` : ''}
        </td>
      </tr>
    `;
  }).join('');
}

function openNewUserModal() {
  document.getElementById('form-user').reset();
  document.getElementById('user-id').value = "";
  document.getElementById('user-username').disabled = false;
  const passEl = document.getElementById('user-password');
  if (passEl) {
    passEl.value = "";
    passEl.placeholder = "Nhập mật khẩu (tối thiểu 6 ký tự)";
  }
  document.getElementById('modal-user-title').innerText = "Tạo Tài Khoản Người Dùng Mới";
  openModal('modal-user');
}

function openEditUserModal(userId) {
  const u = allUsers.find(x => x.id === userId);
  if (!u) return;

  document.getElementById('user-id').value = u.id;
  document.getElementById('user-username').value = u.username;
  document.getElementById('user-username').disabled = true;
  const passEl = document.getElementById('user-password');
  if (passEl) {
    passEl.value = "";
    passEl.placeholder = "Để trống nếu không đổi mật khẩu";
  }
  document.getElementById('user-fullname').value = u.full_name;
  document.getElementById('user-role').value = u.role;
  document.getElementById('user-sbu').value = u.sbu;
  document.getElementById('user-title').value = u.title;
  document.getElementById('user-email').value = u.email || '';
  document.getElementById('user-phone').value = u.phone || '';

  document.getElementById('modal-user-title').innerText = `Chỉnh Sửa Tài Khoản: ${u.full_name}`;
  openModal('modal-user');
}

function onUserRoleChange() {
  const role = document.getElementById('user-role').value;
  const sbuSelect = document.getElementById('user-sbu');
  if (role === 'ADMIN' || role === 'COLLABORATOR') {
    sbuSelect.value = 'ALL';
  } else if (sbuSelect.value === 'ALL') {
    sbuSelect.value = 'SBU1';
  }
}

async function handleUserSubmit(e) {
  e.preventDefault();
  const userId = document.getElementById('user-id').value;
  const isEdit = Boolean(userId);

  const payload = {
    username: document.getElementById('user-username').value.trim(),
    full_name: document.getElementById('user-fullname').value.trim(),
    role: document.getElementById('user-role').value,
    sbu: document.getElementById('user-sbu').value,
    title: document.getElementById('user-title').value.trim(),
    email: document.getElementById('user-email').value.trim(),
    phone: document.getElementById('user-phone').value.trim()
  };

  const passVal = document.getElementById('user-password')?.value?.trim();
  if (passVal) {
    payload.password = passVal;
  } else if (!isEdit) {
    payload.password = '123456';
  }

  try {
    const url = isEdit ? `/api/users/${userId}` : '/api/users';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await authFetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      showToast(isEdit ? "Cập nhật tài khoản thành công!" : "Tạo tài khoản mới thành công!");
      closeModal('modal-user');
      await loadUsers();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi thao tác tài khoản", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

async function deleteUserAccount(userId) {
  const u = allUsers.find(x => x.id === userId);
  if (!u) return;

  if (!confirm(`Bạn có chắc chắn muốn xóa tài khoản ${u.full_name} (@${u.username}) không?`)) {
    return;
  }

  try {
    const res = await authFetch(`/api/users/${userId}`, { method: 'DELETE' });
    if (res.ok) {
      showToast("Đã xóa tài khoản người dùng!");
      await loadUsers();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi xóa tài khoản", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

// ==========================================
// 2. DASHBOARD LOGIC
// ==========================================
async function loadDashboard() {
  try {
    const sbuParam = currentSBU !== 'ALL' ? `?sbu=${currentSBU}` : '';
    const res = await authFetch(`/api/dashboard/metrics${sbuParam}`);
    const data = await res.json();

    // 1. Phễu Cơ Hội & Dự Thầu
    const bidsValEl = document.getElementById('stat-bids-val');
    const bidsCountEl = document.getElementById('stat-bids-count');
    if (bidsValEl) bidsValEl.innerText = formatVND(data.overview.active_bids_value || 0);
    if (bidsCountEl) bidsCountEl.innerText = data.overview.active_bids_count || 0;

    // 2. Giá trị đã trúng thầu (WON)
    const wonValEl = document.getElementById('stat-won-val');
    const wonCountEl = document.getElementById('stat-won-count');
    if (wonValEl) wonValEl.innerText = formatVND(data.overview.won_bids_value || 0);
    if (wonCountEl) wonCountEl.innerText = data.overview.won_bids_count || 0;

    // 3. Khách hàng chiến lược
    const custCountEl = document.getElementById('stat-customer-count');
    const diamondCountEl = document.getElementById('stat-diamond-count');
    const goldCountEl = document.getElementById('stat-gold-count');
    if (custCountEl) custCountEl.innerText = `${data.overview.total_customers || 0} Đối tác`;
    if (diamondCountEl) diamondCountEl.innerText = data.overview.diamond_count || 0;
    if (goldCountEl) goldCountEl.innerText = data.overview.gold_count || 0;

    // 4. Ngân sách CSKH
    const careBudgetEl = document.getElementById('stat-care-budget');
    const careSpentEl = document.getElementById('stat-care-spent');
    if (careBudgetEl) careBudgetEl.innerText = formatVND(data.overview.total_care_budget || 0);
    if (careSpentEl) careSpentEl.innerText = formatVND(data.overview.total_care_spent || 0);

    const matrixSection = document.getElementById('sbu-matrix-section');
    if (currentSBU !== 'ALL') {
      matrixSection.classList.add('hidden');
    } else {
      matrixSection.classList.remove('hidden');
      renderSBUMatrix(data.sbu_matrix);
    }

    renderUrgentTenderDeadlines(data.upcoming_bids || []);
    renderExecutiveReminders(data.executive_reminders);

  } catch (err) {
    console.error("Error loading dashboard metrics:", err);
  }
}

function renderSBUMatrix(matrix) {
  const tbody = document.getElementById('sbu-matrix-body');
  if (!tbody) return;

  tbody.innerHTML = matrix.map(row => `
    <tr class="hover:bg-slate-50 transition">
      <td class="p-3">
        <div class="font-extrabold text-slate-900 flex items-center gap-2">
          <i class="fa-solid ${row.icon} text-slate-500"></i>
          <span>${row.name}</span>
        </div>
      </td>
      <td class="p-3">
        <span class="font-bold text-slate-800">${row.customer_count}</span> đối tác
      </td>
      <td class="p-3">
        <span class="font-bold text-purple-700">${row.bid_count}</span> gói thầu
      </td>
      <td class="p-3 font-black text-purple-700">${formatVND(row.bid_value)}</td>
      <td class="p-3">
        <span class="font-bold text-emerald-600">${formatVND(row.won_value || 0)}</span>
        <div class="text-[10px] text-emerald-700 font-semibold">${row.won_count || 0} gói trúng thầu</div>
      </td>
      <td class="p-3 font-bold text-slate-700">${row.total_bids || (row.bid_count + (row.won_count || 0))} gói thầu</td>
      <td class="p-3 text-right">
        <button onclick="changeSBUFilter('${row.sbu}')" class="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[11px] font-bold cursor-pointer">
          Xem &gt;
        </button>
      </td>
    </tr>
  `).join('');
}

function renderUrgentTenderDeadlines(bids) {
  const container = document.getElementById('urgent-milestones-list');
  if (!container) return;

  if (!bids || bids.length === 0) {
    container.innerHTML = '<div class="text-xs text-slate-400 py-4 text-center">Không có hồ sơ thầu nào sắp đến hạn nộp.</div>';
    return;
  }

  container.innerHTML = bids.map(b => {
    const cls = classifyFeconProject(b.estimated_value);
    return `
      <div onclick="openBidDetailModal(${b.id})" class="p-3 bg-purple-50/50 hover:bg-purple-50 rounded-xl border border-purple-200/80 flex items-center justify-between gap-3 transition cursor-pointer hover:border-purple-400" title="Bấm để xem chi tiết hồ sơ thầu">
        <div class="space-y-0.5 min-w-0">
          <div class="flex items-center gap-1.5 flex-wrap">
            ${getSBUBadge(b.sbu)}
            <span class="px-1.5 py-0.5 rounded text-[9px] font-black border ${cls.colorClass}">${cls.badge}</span>
            <span class="font-bold text-slate-900 text-xs truncate max-w-[200px]">${b.project_title}</span>
          </div>
          <div class="text-[11px] text-slate-600 truncate">
            CĐT: <b class="text-slate-800">${b.customer_name}</b> • Người liên hệ: ${b.key_decision_maker || 'Chưa cập nhật'}
          </div>
          <div class="text-[10px] text-indigo-900 font-semibold">
            Thẩm quyền duyệt CSKH: <b class="font-bold">${cls.approver_short}</b>
          </div>
        </div>
        <div class="text-right shrink-0">
          <div class="font-black text-purple-700 text-xs">${formatVND(b.estimated_value)}</div>
          <div class="text-[10px] font-bold text-rose-600 mt-0.5">
            <i class="fa-solid fa-clock mr-0.5"></i> Hạn: ${b.tender_deadline || 'Sớm'}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function renderExecutiveReminders(reminders) {
  const container = document.getElementById('executive-reminders-list');
  if (!container) return;

  if (!reminders || reminders.length === 0) {
    container.innerHTML = '<div class="text-xs text-slate-400 py-4 text-center">Không có sự kiện ngoại giao trong 60 ngày tới.</div>';
    return;
  }

  container.innerHTML = reminders.map(r => `
    <div class="p-3 bg-amber-50/60 rounded-xl border border-amber-200/60 flex items-center justify-between gap-3">
      <div class="space-y-0.5">
        <div class="flex items-center gap-2">
          ${getSBUBadge(r.sbu)}
          <span class="font-bold text-slate-900 text-xs">${r.event_title}</span>
        </div>
        <div class="text-[11px] text-slate-600">${r.name} (${r.decision_maker_role || 'Lãnh đạo'})</div>
        <div class="text-[10px] text-amber-800 font-bold">Ngày sự kiện: ${r.target_date} (còn ${r.days_until} ngày)</div>
      </div>
      <button onclick="prepareZaloGreeting('${r.id}', '${r.key_decision_maker}', '${r.decision_maker_phone || ''}', '${r.name}')" class="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-slate-950 rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm whitespace-nowrap">
        <i class="fa-solid fa-gift"></i> Gửi Lời Chúc
      </button>
    </div>
  `).join('');
}

// ==========================================
// 3. STRATEGIC CUSTOMERS & DUPLICATE REUSE
// ==========================================
let searchCustTimeout = null;
function debounceCustomerSearch() {
  clearTimeout(searchCustTimeout);
  searchCustTimeout = setTimeout(loadCustomers, 300);
}

// Real-time Duplicate Detection
let dupScanTimeout = null;
function triggerDuplicateScan() {
  clearTimeout(dupScanTimeout);
  dupScanTimeout = setTimeout(async () => {
    const tax = document.getElementById('cust-tax')?.value || "";
    const phone = document.getElementById('cust-decision-phone')?.value || "";
    const name = document.getElementById('cust-name')?.value || "";

    if (tax.length < 5 && phone.length < 8 && name.length < 4) {
      document.getElementById('duplicate-warning-banner').classList.add('hidden');
      return;
    }

    try {
      const res = await authFetch(`/api/customers/check-duplicate?tax_code=${encodeURIComponent(tax)}&phone=${encodeURIComponent(phone)}&name=${encodeURIComponent(name)}`);
      const data = await res.json();

      const banner = document.getElementById('duplicate-warning-banner');
      const infoEl = document.getElementById('duplicate-matched-info');

      if (data.found && data.matches.length > 0) {
        lastMatchedCustomer = data.matches[0];
        infoEl.innerHTML = `
          <div class="font-bold text-slate-900">${lastMatchedCustomer.name} (Mã: ${lastMatchedCustomer.code})</div>
          <div class="text-slate-600 mt-0.5">
            <b>MST:</b> ${lastMatchedCustomer.tax_code || 'N/A'} • 
            <b>Khối hiện tại:</b> ${getSBUBadge(lastMatchedCustomer.sbu)} • 
            <b>Lãnh đạo:</b> ${lastMatchedCustomer.key_decision_maker} (${lastMatchedCustomer.decision_maker_phone || 'N/A'})
          </div>
        `;
        banner.classList.remove('hidden');
      } else {
        banner.classList.add('hidden');
      }
    } catch (e) {
      console.error(e);
    }
  }, 400);
}

// Reuse existing matched customer profile
function reuseMatchedCustomer() {
  if (!lastMatchedCustomer) return;

  document.getElementById('cust-name').value = lastMatchedCustomer.name;
  document.getElementById('cust-tax').value = lastMatchedCustomer.tax_code || '';
  document.getElementById('cust-tier').value = lastMatchedCustomer.tier || 'STRATEGIC_VIP';
  document.getElementById('cust-segment').value = lastMatchedCustomer.segment || 'B2B';
  document.getElementById('cust-headquarters').value = lastMatchedCustomer.headquarters || '';
  document.getElementById('cust-decision-maker').value = lastMatchedCustomer.key_decision_maker || '';
  document.getElementById('cust-decision-role').value = lastMatchedCustomer.decision_maker_role || '';
  document.getElementById('cust-decision-phone').value = lastMatchedCustomer.decision_maker_phone || '';
  document.getElementById('cust-decision-bday').value = lastMatchedCustomer.decision_maker_birthday || '';
  document.getElementById('cust-anniversary').value = lastMatchedCustomer.founding_anniversary || '';
  document.getElementById('cust-notes').value = lastMatchedCustomer.strategic_notes || `Sử dụng lại hồ sơ từ ${lastMatchedCustomer.sbu}`;

  document.getElementById('duplicate-warning-banner').classList.add('hidden');
  showToast(`Đã đồng bộ thông tin đối tác ${lastMatchedCustomer.name}! Nhấn "Lưu" để liên kết vào SBU của bạn.`);
}

function getTierBadge(tier, totalScore, vetoApplied, isSpecial) {
  let badge = '';
  if (tier === 'DIAMOND' || tier === 'STRATEGIC_VIP') {
    badge = `<span class="px-2 py-0.5 rounded-lg text-[10px] font-black bg-cyan-100 text-cyan-800 border border-cyan-300 shadow-sm">💎 Kim Cương</span>`;
  } else if (tier === 'GOLD' || tier === 'CLOSE_PARTNER') {
    badge = `<span class="px-2 py-0.5 rounded-lg text-[10px] font-black bg-amber-100 text-amber-900 border border-amber-300 shadow-sm">🥇 Vàng</span>`;
  } else {
    badge = `<span class="px-2 py-0.5 rounded-lg text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-300 shadow-sm">🥈 Bạc</span>`;
  }

  let extras = '';
  if (vetoApplied) {
    extras += `<span class="text-[9px] text-rose-600 font-bold block mt-0.5" title="Bị giới hạn Hạng Vàng do Tiêu chí 3 Năng lực tài chính = 0đ">⚠️ Phủ Quyết</span>`;
  } else if (isSpecial) {
    extras += `<span class="text-[9px] text-purple-600 font-bold block mt-0.5" title="Đặc cách bởi CT HĐQT/TGĐ">👑 Đặc Cách</span>`;
  }
  return `<div>${badge}${extras}</div>`;
}

async function loadCustomers() {
  const search = document.getElementById('cust-search')?.value || "";
  const sbuFilter = document.getElementById('cust-filter-sbu')?.value || currentSBU;
  const tierFilter = document.getElementById('cust-filter-tier')?.value || "";

  let url = `/api/customers?`;
  if (sbuFilter && sbuFilter !== 'ALL') url += `sbu=${sbuFilter}&`;
  if (tierFilter) url += `tier=${tierFilter}&`;
  if (search) url += `search=${encodeURIComponent(search)}&`;

  try {
    const res = await authFetch(url);
    const data = await res.json();
    allCustomers = data;

    const tbody = document.getElementById('customers-table-body');
    const hint = document.getElementById('cust-permission-hint');
    if (hint) {
      if (currentUser.role === 'ADMIN') {
        hint.innerHTML = '<span class="text-amber-600 font-bold">👑 Ban Lãnh Đạo: Toàn quyền Tạo, Sửa, Đánh giá, Xóa cả 5 SBU</span>';
      } else if (currentUser.role === 'COLLABORATOR') {
        hint.innerHTML = `<span class="text-emerald-600 font-bold">🤝 Cộng Tác Viên (${currentUser.sbu === 'ALL' ? 'Toàn quốc' : currentUser.sbu}): Được phép Giới thiệu & Thêm mới đối tác</span>`;
      } else {
        hint.innerHTML = `<span class="text-blue-600 font-bold">💼 Giám Đốc KD ${currentUser.sbu}: Quản lý, Tạo, Sửa & Chấm điểm khách hàng thuộc ${currentUser.sbu}</span>`;
      }
    }

    if (!tbody) return;

    if (data.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-slate-400 text-xs">Không có đối tác phù hợp.</td></tr>';
      return;
    }

    tbody.innerHTML = data.map(c => {
      const isCTV = currentUser.role === 'COLLABORATOR';
      const canEdit = currentUser.role === 'ADMIN' || (currentUser.role === 'SBU_DIRECTOR' && currentUser.sbu === c.sbu);
      const canDelete = currentUser.role === 'ADMIN';

      const annualBudget = c.annual_care_budget || 0;
      const spentBudget = c.spent_care_budget || 0;
      const budgetPct = annualBudget > 0 ? Math.min(100, Math.round((spentBudget / annualBudget) * 100)) : 0;

      return `
        <tr class="hover:bg-slate-50 transition cursor-pointer" onclick="viewCustomer360(${c.id})">
          <td class="p-3.5">
            <div class="font-black text-slate-900">${c.name}</div>
            <div class="text-[11px] text-slate-400 font-mono">${c.code} ${c.tax_code ? `• MST: ${c.tax_code}` : ''} • ${c.segment}</div>
          </td>
          <td class="p-3.5">
            ${getSBUBadge(c.sbu)}
          </td>
          <td class="p-3.5">
            <div class="font-bold text-slate-900">${c.key_decision_maker}</div>
            <div class="text-[11px] text-slate-500">${c.decision_maker_role || ''} • <span class="text-blue-600 font-semibold">${c.decision_maker_phone || c.phone || ''}</span></div>
          </td>
          <td class="p-3.5">
            ${getTierBadge(c.tier, c.total_score, c.veto_applied, c.is_special_elevated)}
            <div class="text-[11px] font-black text-slate-700 mt-1">
              <span class="text-blue-600">${c.total_score !== null && c.total_score !== undefined ? c.total_score : 100}</span><span class="text-slate-400 font-normal">/100đ</span>
            </div>
          </td>
          <td class="p-3.5">
            <div class="font-black text-slate-900 text-xs">${formatVND(annualBudget)}</div>
            <div class="text-[10px] text-slate-500 flex items-center justify-between gap-2 mt-0.5">
              <span>Đã chi: <b class="text-slate-800">${formatVND(spentBudget)}</b></span>
              <span class="font-bold ${budgetPct > 80 ? 'text-rose-600' : 'text-emerald-600'}">${budgetPct}%</span>
            </div>
            <div class="w-24 bg-slate-100 rounded-full h-1.5 mt-1 overflow-hidden">
              <div class="h-1.5 rounded-full ${budgetPct > 80 ? 'bg-rose-500' : 'bg-emerald-500'}" style="width: ${budgetPct}%"></div>
            </div>
            <div class="text-[10px] text-slate-400 mt-1 truncate max-w-[130px]" title="${c.in_charge_executive || ''}">
              <i class="fa-solid fa-user-tie text-[9px] mr-0.5"></i> ${c.in_charge_executive || 'Chưa phân công'}
            </div>
          </td>
          <td class="p-3.5">
            <div class="font-black text-slate-900">${formatVND(c.total_contract_value)}</div>
            <div class="text-[11px] text-slate-500">${c.project_count || 0} dự án đang theo dõi</div>
          </td>
          <td class="p-3.5 text-right space-x-1" onclick="event.stopPropagation()">
            <button onclick="viewCustomer360(${c.id})" class="p-1.5 text-slate-500 hover:text-blue-600 transition" title="Xem Hồ Sơ 360°">
              <i class="fa-solid fa-eye"></i>
            </button>
            ${canEdit ? `
              <button onclick="openAssessmentModal(${c.id})" class="p-1.5 text-slate-500 hover:text-amber-600 transition" title="Chấm Điểm & Phân Hạng FECON (5 Tiêu Chí)">
                <i class="fa-solid fa-award text-amber-500"></i>
              </button>
              <button onclick="openEditCustomerModal(${c.id})" class="p-1.5 text-slate-500 hover:text-blue-600 transition" title="Chỉnh sửa thông tin">
                <i class="fa-solid fa-pen-to-square"></i>
              </button>
            ` : (!isCTV ? `
              <span class="p-1.5 text-slate-300 cursor-not-allowed" title="Chỉ GĐKD ${c.sbu} mới được sửa"><i class="fa-solid fa-lock"></i></span>
            ` : '')}
            ${canDelete ? `
              <button onclick="deleteCustomer(${c.id})" class="p-1.5 text-slate-500 hover:text-rose-600 transition" title="Xóa đối tác (Admin)">
                <i class="fa-solid fa-trash"></i>
              </button>
            ` : ''}
          </td>
        </tr>
      `;
    }).join('');

  } catch (err) {
    console.error("Error loading customers:", err);
  }
}

async function viewCustomer360(id) {
  try {
    const res = await authFetch(`/api/customers/${id}`);
    const c = await res.json();

    document.getElementById('view-cust-code').innerText = c.code;
    document.getElementById('view-cust-name').innerText = c.name;

    const annualBudget = c.annual_care_budget || 0;
    const spentBudget = c.spent_care_budget || 0;
    const budgetPct = annualBudget > 0 ? Math.min(100, Math.round((spentBudget / annualBudget) * 100)) : 0;

    const content = document.getElementById('customer-detail-content');
    content.innerHTML = `
      <!-- Top Overview Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
          <div class="text-slate-400 font-bold uppercase text-[10px]">Phân loại SBU & Hạng FECON</div>
          <div class="mt-1 flex items-center gap-1.5">${getSBUBadge(c.sbu)} ${getTierBadge(c.tier, c.total_score, c.veto_applied, c.is_special_elevated)}</div>
          <div class="text-xs text-slate-700 font-bold mt-2">Phân khúc: ${c.segment} • Điểm: <span class="text-blue-600 font-black">${c.total_score || 0}/100đ</span></div>
        </div>
        <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
          <div class="text-slate-400 font-bold uppercase text-[10px]">Lãnh đạo then chốt</div>
          <div class="font-extrabold text-slate-900 text-xs mt-1">${c.key_decision_maker}</div>
          <div class="text-[11px] text-slate-600">${c.decision_maker_role || ''}</div>
          <div class="text-[11px] text-blue-600 font-bold mt-1"><i class="fa-solid fa-phone mr-1"></i> ${c.decision_maker_phone || c.phone}</div>
        </div>
        <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
          <div class="text-slate-400 font-bold uppercase text-[10px]">Sự kiện ngoại giao</div>
          <div class="text-xs text-slate-800 mt-1"><i class="fa-solid fa-cake-candles text-amber-500 mr-1"></i> Sinh nhật: ${c.decision_maker_birthday || 'Chưa cập nhật'}</div>
          <div class="text-xs text-slate-800 mt-1"><i class="fa-solid fa-building text-blue-500 mr-1"></i> Thành lập: ${c.founding_anniversary || 'Chưa cập nhật'}</div>
        </div>
      </div>

      <!-- FECON Policy Tiering & Budget Tracking Card -->
      <div class="p-4 rounded-2xl border border-amber-200 bg-gradient-to-r from-amber-50/60 to-white space-y-3">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-amber-100 pb-2.5">
          <div>
            <div class="text-[10px] font-black text-amber-800 uppercase tracking-wider">Hồ Sơ Đánh Giá Phân Hạng FECON (FECON-CSCSKH/ĐT-01)</div>
            <div class="font-black text-slate-900 text-sm flex items-center gap-2 mt-0.5">
              <span>Hạng: <b>${c.tier === 'DIAMOND' ? '💎 Kim Cương' : (c.tier === 'GOLD' ? '🥇 Vàng' : '🥈 Bạc')}</b></span>
              <span class="text-slate-400">•</span>
              <span>Tổng Điểm: <b class="text-blue-600">${c.total_score || 0}/100</b></span>
              ${c.veto_applied ? '<span class="px-2 py-0.5 bg-rose-600 text-white rounded text-[10px] font-black">ÁP DỤNG PHỦ QUYẾT</span>' : ''}
              ${c.is_special_elevated ? '<span class="px-2 py-0.5 bg-purple-600 text-white rounded text-[10px] font-black">ĐẶC CÁCH LÃNH ĐẠO</span>' : ''}
            </div>
          </div>
          <button onclick="openAssessmentModal(${c.id})" class="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-slate-950 rounded-xl font-black text-xs shadow-sm flex items-center gap-1.5 transition">
            <i class="fa-solid fa-award"></i> Đánh Giá Lại (5 Tiêu Chí)
          </button>
        </div>

        <!-- 5 Criteria Breakdown -->
        <div class="grid grid-cols-2 sm:grid-cols-5 gap-2 text-center text-xs">
          <div class="p-2 bg-white rounded-xl border border-slate-200">
            <div class="text-[10px] text-slate-500 font-bold">1. Quy mô (15)</div>
            <div class="font-black text-slate-900 text-sm mt-0.5">${c.score_scale_project || 0}đ</div>
          </div>
          <div class="p-2 bg-white rounded-xl border border-slate-200">
            <div class="text-[10px] text-slate-500 font-bold">2. Phù hợp (25)</div>
            <div class="font-black text-slate-900 text-sm mt-0.5">${c.score_fecon_fit || 0}đ</div>
          </div>
          <div class="p-2 bg-white rounded-xl border ${c.score_financial_capacity === 0 ? 'border-rose-300 bg-rose-50' : 'border-slate-200'}">
            <div class="text-[10px] ${c.score_financial_capacity === 0 ? 'text-rose-700 font-black' : 'text-slate-500 font-bold'}">3. Tài chính (25)</div>
            <div class="font-black ${c.score_financial_capacity === 0 ? 'text-rose-600' : 'text-slate-900'} text-sm mt-0.5">${c.score_financial_capacity || 0}đ</div>
          </div>
          <div class="p-2 bg-white rounded-xl border border-slate-200">
            <div class="text-[10px] text-slate-500 font-bold">4. Lịch sử (20)</div>
            <div class="font-black text-slate-900 text-sm mt-0.5">${c.score_cooperation_history || 0}đ</div>
          </div>
          <div class="p-2 bg-white rounded-xl border border-slate-200">
            <div class="text-[10px] text-slate-500 font-bold">5. Quản lý (15)</div>
            <div class="font-black text-slate-900 text-sm mt-0.5">${c.score_management_capacity || 0}đ</div>
          </div>
        </div>

        <!-- Budget & Care Guidelines -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-white p-3 rounded-xl border border-amber-200 text-xs">
          <div>
            <div class="text-slate-400 font-bold text-[10px] uppercase">Ngân Sách CSKH Thường Niên</div>
            <div class="text-base font-black text-emerald-700 mt-0.5">${formatVND(annualBudget)}</div>
            <div class="text-[10px] text-slate-500 mt-1">Đã chi: <b>${formatVND(spentBudget)}</b> (${budgetPct}%)</div>
            <div class="w-full bg-slate-100 rounded-full h-1.5 mt-1 overflow-hidden">
              <div class="h-1.5 rounded-full ${budgetPct > 80 ? 'bg-rose-500' : 'bg-emerald-500'}" style="width: ${budgetPct}%"></div>
            </div>
          </div>
          <div>
            <div class="text-slate-400 font-bold text-[10px] uppercase">Cấp Phụ Trách Theo Chính Sách</div>
            <div class="font-bold text-slate-900 mt-1">${c.in_charge_executive || 'Chưa phân công'}</div>
          </div>
          <div>
            <div class="text-slate-400 font-bold text-[10px] uppercase">Tần Suất Tiếp Khách Định Kỳ</div>
            <div class="font-bold text-slate-900 mt-1">${c.care_frequency || 'Theo sự vụ'}</div>
          </div>
        </div>
      </div>

      <div>
        <div class="flex items-center justify-between mb-2">
          <h4 class="font-black text-xs text-slate-900 uppercase flex items-center gap-1.5">
            <i class="fa-solid fa-filter-circle-dollar text-purple-600"></i> Các Cơ Hội & Hồ Sơ Dự Thầu Đang Theo Đuổi (${c.bids ? c.bids.length : 0})
          </h4>
          <button onclick="closeModal('modal-customer-detail'); openNewBidModal()" class="px-2.5 py-1 bg-purple-50 hover:bg-purple-100 text-purple-700 font-bold text-[11px] rounded-lg transition cursor-pointer flex items-center gap-1">
            <i class="fa-solid fa-plus"></i> Thêm Gói Thầu Cho CĐT Này
          </button>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          ${c.bids && c.bids.length > 0 ? c.bids.map(b => {
            const cls = classifyFeconProject(b.estimated_value);
            return `
              <div onclick="closeModal('modal-customer-detail'); openBidDetailModal(${b.id})" class="p-3 bg-white rounded-xl border border-slate-200 hover:border-purple-400 cursor-pointer transition shadow-2xs space-y-2">
                <div class="flex items-center justify-between gap-1">
                  <div class="font-bold text-slate-900 text-xs truncate">${b.project_title}</div>
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-black border shrink-0 ${cls.colorClass}">
                    ${cls.badge}
                  </span>
                </div>
                <div class="text-[11px] text-slate-500 flex justify-between">
                  <span>Giai đoạn: <b class="text-purple-700">${b.stage}</b> (${b.win_rate || 50}% Win)</span>
                  <span class="font-black text-purple-700">${formatVND(b.estimated_value)}</span>
                </div>
                <div class="text-[10px] text-indigo-900 font-semibold flex items-center justify-between pt-1 border-t border-slate-100">
                  <span>Duyệt CSKH: <b class="font-bold">${cls.approver_authority}</b></span>
                  <span class="text-slate-400">Hạn: ${b.tender_deadline || 'N/A'}</span>
                </div>
              </div>
            `;
          }).join('') : '<div class="col-span-2 text-slate-400 text-xs py-4 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">Chưa có hồ sơ thầu nào cho đối tác này.</div>'}
        </div>
      </div>

      <div>
        <h4 class="font-black text-xs text-slate-900 uppercase mb-2">Nhật Ký Ngoại Giao & Tiếp Khách Gần Nhất</h4>
        <div class="space-y-2">
          ${c.activities && c.activities.length > 0 ? c.activities.map(a => `
            <div class="p-2.5 bg-slate-50 rounded-xl border border-slate-200 text-xs">
              <div class="flex items-center justify-between font-bold text-slate-800">
                <span>${a.title}</span>
                <span class="text-slate-400 font-mono text-[10px]">${a.occurred_at}</span>
              </div>
              <p class="text-slate-600 mt-1">${a.content || ''}</p>
              <div class="flex justify-between items-center text-[10px] mt-1.5 pt-1.5 border-t border-slate-100">
                <span class="text-blue-600 font-semibold">Lãnh đạo: ${a.leader_in_charge}</span>
                <span class="text-emerald-700 font-bold">${a.cost ? 'Chi phí: ' + formatVND(a.cost) : ''}</span>
              </div>
            </div>
          `).join('') : '<div class="text-slate-400 text-xs">Chưa có hoạt động tiếp khách ghi nhận.</div>'}
        </div>
      </div>
    `;

    openModal('modal-customer-detail');

  } catch (err) {
    console.error("Error viewing customer:", err);
    showToast("Không thể tải thông tin đối tác", "error");
  }
}

// ==========================================
// FECON POLICY & CUSTOMER ASSESSMENT LOGIC
// ==========================================
let currentAssessment = {
  customerId: null,
  scores: { 1: 15.0, 2: 25.0, 3: 25.0, 4: 20.0, 5: 15.0 },
  isSpecialElevated: false,
  notes: ''
};

function openPolicyModal() {
  openModal('modal-fecon-policy');
}

function openAssessmentModal(id) {
  const cust = allCustomers.find(c => c.id === id);
  if (!cust) return;

  currentAssessment.customerId = id;
  currentAssessment.scores[1] = cust.score_scale_project !== null && cust.score_scale_project !== undefined ? cust.score_scale_project : 15.0;
  currentAssessment.scores[2] = cust.score_fecon_fit !== null && cust.score_fecon_fit !== undefined ? cust.score_fecon_fit : 25.0;
  currentAssessment.scores[3] = cust.score_financial_capacity !== null && cust.score_financial_capacity !== undefined ? cust.score_financial_capacity : 25.0;
  currentAssessment.scores[4] = cust.score_cooperation_history !== null && cust.score_cooperation_history !== undefined ? cust.score_cooperation_history : 20.0;
  currentAssessment.scores[5] = cust.score_management_capacity !== null && cust.score_management_capacity !== undefined ? cust.score_management_capacity : 15.0;
  currentAssessment.isSpecialElevated = Boolean(cust.is_special_elevated);
  currentAssessment.notes = cust.strategic_notes || '';

  document.getElementById('assess-customer-id').value = id;
  document.getElementById('assess-modal-customer-name').innerText = `${cust.name} (${cust.code} • ${cust.sbu})`;
  document.getElementById('assess-notes').value = currentAssessment.notes;
  document.getElementById('assess-special-elevated').checked = currentAssessment.isSpecialElevated;

  // Update button highlights
  updateCriteriaButtons(1, currentAssessment.scores[1]);
  updateCriteriaButtons(2, currentAssessment.scores[2]);
  updateCriteriaButtons(3, currentAssessment.scores[3]);
  updateCriteriaButtons(4, currentAssessment.scores[4]);
  updateCriteriaButtons(5, currentAssessment.scores[5]);

  recalculateLiveAssessment();
  openModal('modal-customer-assessment');
}

function updateCriteriaButtons(critNum, score) {
  const suffix = score === 15.0 ? '15' : (score === 7.5 ? '75' : (score === 25.0 ? '25' : (score === 12.5 ? '125' : (score === 20.0 ? '20' : (score === 10.0 ? '10' : '0')))));
  
  // Reset all buttons for this criteria
  const allBtns = document.querySelectorAll(`[id^="btn-c${critNum}-"]`);
  allBtns.forEach(btn => {
    btn.className = 'p-2 border rounded-lg text-left transition font-semibold bg-white border-slate-200 text-slate-700 hover:border-slate-300';
  });

  const activeBtn = document.getElementById(`btn-c${critNum}-${suffix}`);
  if (activeBtn) {
    if (critNum === 3 && score === 0.0) {
      activeBtn.className = 'p-2 border-2 border-rose-500 bg-rose-100 rounded-lg text-left transition font-black text-rose-900 shadow-sm';
    } else {
      activeBtn.className = 'p-2 border-2 border-amber-500 bg-amber-50 rounded-lg text-left transition font-black text-amber-950 shadow-sm';
    }
  }

  const label = document.getElementById(`label-score-c${critNum}`);
  if (label) label.innerText = `${score}đ`;
}

function setCriteriaScore(critNum, score) {
  currentAssessment.scores[critNum] = score;
  updateCriteriaButtons(critNum, score);
  recalculateLiveAssessment();
}

function recalculateLiveAssessment() {
  const s1 = currentAssessment.scores[1] || 0;
  const s2 = currentAssessment.scores[2] || 0;
  const s3 = currentAssessment.scores[3] || 0;
  const s4 = currentAssessment.scores[4] || 0;
  const s5 = currentAssessment.scores[5] || 0;
  const total = Math.round((s1 + s2 + s3 + s4 + s5) * 10) / 10;

  const isSpecial = document.getElementById('assess-special-elevated')?.checked || false;
  currentAssessment.isSpecialElevated = isSpecial;

  document.getElementById('assess-total-score').innerText = total.toFixed(1);

  const predictedBadge = document.getElementById('assess-predicted-badge');
  const vetoBadge = document.getElementById('assess-veto-badge');
  const specialBadge = document.getElementById('assess-special-badge');
  const vetoBanner = document.getElementById('assess-veto-banner');
  const budgetText = document.getElementById('assess-budget-text');
  const inchargeText = document.getElementById('assess-incharge-text');
  const frequencyText = document.getElementById('assess-frequency-text');

  let tier = 'SILVER';
  let tierName = '🥈 Hạng Bạc';
  let budgetStr = '5.000.000 VNĐ';
  let inchargeStr = 'Cấp phụ trách: <b>SBU Leader / GĐKD phụ trách</b>';
  let freqStr = 'Tần suất: Theo sự vụ thực tế';

  if (isSpecial) {
    tier = 'DIAMOND';
    tierName = '💎 Hạng Kim Cương';
    budgetStr = '80.000.000 VNĐ';
    inchargeStr = 'Cấp phụ trách: <b>Chủ tịch HĐQT / TGĐ trực tiếp phụ trách (Đặc cách)</b>';
    freqStr = 'Tần suất: 1 tháng / lần';
    if (vetoBadge) vetoBadge.classList.add('hidden');
    if (specialBadge) specialBadge.classList.remove('hidden');
    if (vetoBanner) vetoBanner.classList.add('hidden');
  } else if (s3 <= 0.0) {
    // Veto triggered!
    if (vetoBanner) vetoBanner.classList.remove('hidden');
    if (specialBadge) specialBadge.classList.add('hidden');
    if (total >= 50.0) {
      tier = 'GOLD';
      tierName = '🥇 Hạng Vàng';
      budgetStr = '20.000.000 VNĐ';
      inchargeStr = 'Cấp phụ trách: <b>TGĐ / SBU Leader phụ trách</b>';
      freqStr = 'Tần suất: 3 tháng / lần';
      if (total >= 80.0) {
        if (vetoBadge) vetoBadge.classList.remove('hidden');
      } else {
        if (vetoBadge) vetoBadge.classList.add('hidden');
      }
    } else {
      if (vetoBadge) vetoBadge.classList.add('hidden');
    }
  } else {
    if (vetoBanner) vetoBanner.classList.add('hidden');
    if (vetoBadge) vetoBadge.classList.add('hidden');
    if (specialBadge) specialBadge.classList.add('hidden');

    if (total >= 80.0) {
      tier = 'DIAMOND';
      tierName = '💎 Hạng Kim Cương';
      budgetStr = '80.000.000 VNĐ';
      inchargeStr = 'Cấp phụ trách: <b>Chủ tịch HĐQT / TGĐ trực tiếp phụ trách</b>';
      freqStr = 'Tần suất: 1 tháng / lần';
    } else if (total >= 50.0) {
      tier = 'GOLD';
      tierName = '🥇 Hạng Vàng';
      budgetStr = '20.000.000 VNĐ';
      inchargeStr = 'Cấp phụ trách: <b>TGĐ / SBU Leader phụ trách</b>';
      freqStr = 'Tần suất: 3 tháng / lần';
    }
  }

  if (predictedBadge) {
    predictedBadge.innerText = tierName;
    if (tier === 'DIAMOND') predictedBadge.className = 'px-3 py-1 bg-cyan-600 text-white font-black text-xs rounded-xl shadow-sm uppercase';
    else if (tier === 'GOLD') predictedBadge.className = 'px-3 py-1 bg-amber-500 text-slate-950 font-black text-xs rounded-xl shadow-sm uppercase';
    else predictedBadge.className = 'px-3 py-1 bg-slate-600 text-white font-black text-xs rounded-xl shadow-sm uppercase';
  }

  if (budgetText) budgetText.innerText = budgetStr;
  if (inchargeText) inchargeText.innerHTML = inchargeStr;
  if (frequencyText) frequencyText.innerText = freqStr;
}

async function saveCustomerAssessment() {
  if (!currentAssessment.customerId) return;

  const payload = {
    score_scale_project: currentAssessment.scores[1],
    score_fecon_fit: currentAssessment.scores[2],
    score_financial_capacity: currentAssessment.scores[3],
    score_cooperation_history: currentAssessment.scores[4],
    score_management_capacity: currentAssessment.scores[5],
    is_special_elevated: document.getElementById('assess-special-elevated')?.checked || false,
    strategic_notes: document.getElementById('assess-notes')?.value || ""
  };

  try {
    const res = await authFetch(`/api/customers/${currentAssessment.customerId}/assess`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const data = await res.json();
      showToast(data.message || "Đã lưu kết quả phân hạng khách hàng!");
      closeModal('modal-customer-assessment');
      loadCustomers();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi lưu đánh giá", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối khi lưu đánh giá", "error");
  }
}

function openNewCustomerModal() {
  document.getElementById('form-customer').reset();
  document.getElementById('cust-id').value = "";
  document.getElementById('duplicate-warning-banner').classList.add('hidden');
  document.getElementById('modal-customer-title').innerText = "Thêm Đối Tác / Chủ Đầu Tư Chiến Lược";

  const sbuSelect = document.getElementById('cust-sbu');
  const sbuHint = document.getElementById('cust-sbu-hint');

  if (currentUser.role === 'SBU_DIRECTOR') {
    sbuSelect.value = currentUser.sbu;
    sbuSelect.disabled = true;
    sbuHint.innerText = `🔒 Bạn là GĐKD ${currentUser.sbu}: Khách hàng mới sẽ tự động được lưu trữ vào ${currentUser.sbu}.`;
    document.getElementById('modal-customer-title').innerText = `Thêm Đối Tác Chiến Lược (${currentUser.sbu})`;
  } else if (currentUser.role === 'COLLABORATOR') {
    sbuSelect.disabled = false;
    if (currentUser.sbu !== 'ALL') sbuSelect.value = currentUser.sbu;
    sbuHint.innerText = `🤝 Bạn là Cộng Tác Viên: Nhập thông tin đối tác & dự án tiềm năng để chuyển tiếp cho Ban Lãnh Đạo & GĐKD SBU tiếp nhận.`;
    document.getElementById('modal-customer-title').innerText = "🤝 Giới Thiệu Đối Tác / Cơ Hội Mới (Cộng Tác Viên)";
  } else {
    sbuSelect.disabled = false;
    sbuHint.innerText = `👑 Ban Lãnh Đạo: Có thể gán đối tác cho bất kỳ khối SBU nào.`;
    document.getElementById('modal-customer-title').innerText = "Thêm Đối Tác / Chủ Đầu Tư Chiến Lược";
  }

  openModal('modal-customer');
}

function openEditCustomerModal(id) {
  const c = allCustomers.find(x => x.id === id);
  if (!c) return;

  document.getElementById('cust-id').value = c.id;
  document.getElementById('cust-name').value = c.name;
  document.getElementById('cust-tax').value = c.tax_code || '';
  document.getElementById('cust-tier').value = (c.tier === 'STRATEGIC_VIP' ? 'DIAMOND' : (c.tier === 'CLOSE_PARTNER' ? 'GOLD' : (c.tier === 'PROSPECT' ? 'SILVER' : c.tier)));
  document.getElementById('cust-segment').value = c.segment;
  document.getElementById('cust-headquarters').value = c.headquarters || '';
  document.getElementById('cust-decision-maker').value = c.key_decision_maker;
  document.getElementById('cust-decision-role').value = c.decision_maker_role || '';
  document.getElementById('cust-decision-phone').value = c.decision_maker_phone || '';
  document.getElementById('cust-decision-bday').value = c.decision_maker_birthday || '';
  document.getElementById('cust-anniversary').value = c.founding_anniversary || '';
  document.getElementById('cust-notes').value = c.strategic_notes || '';

  const sbuSelect = document.getElementById('cust-sbu');
  sbuSelect.value = c.sbu;
  if (currentUser.role === 'SBU_DIRECTOR') {
    sbuSelect.disabled = true;
  } else {
    sbuSelect.disabled = false;
  }

  document.getElementById('duplicate-warning-banner').classList.add('hidden');
  document.getElementById('modal-customer-title').innerText = `Chỉnh Sửa Đối Tác: ${c.name}`;
  openModal('modal-customer');
}

async function handleCustomerSubmit(e) {
  e.preventDefault();
  const custId = document.getElementById('cust-id').value;
  const isEdit = Boolean(custId);
  const sbuSelect = document.getElementById('cust-sbu');

  const payload = {
    name: document.getElementById('cust-name').value,
    sbu: currentUser.role === 'SBU_DIRECTOR' ? currentUser.sbu : sbuSelect.value,
    tier: document.getElementById('cust-tier').value,
    segment: document.getElementById('cust-segment').value,
    tax_code: document.getElementById('cust-tax').value,
    headquarters: document.getElementById('cust-headquarters').value,
    key_decision_maker: document.getElementById('cust-decision-maker').value,
    decision_maker_role: document.getElementById('cust-decision-role').value,
    decision_maker_phone: document.getElementById('cust-decision-phone').value,
    decision_maker_birthday: document.getElementById('cust-decision-bday').value,
    founding_anniversary: document.getElementById('cust-anniversary').value,
    strategic_notes: document.getElementById('cust-notes').value
  };

  try {
    const url = isEdit ? `/api/customers/${custId}` : '/api/customers';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await authFetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      showToast(isEdit ? "Cập nhật đối tác thành công!" : "Lưu trữ đối tác thành công vào hệ thống!");
      closeModal('modal-customer');
      loadCustomers();
      loadDashboard();
      populateCustomerSelects();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi lưu đối tác", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

async function deleteCustomer(id) {
  if (currentUser.role !== 'ADMIN') {
    showToast("Chỉ Ban Lãnh Đạo (Admin) mới có quyền xóa khách hàng!", "error");
    return;
  }

  const c = allCustomers.find(x => x.id === id);
  if (!confirm(`Bạn có chắc chắn muốn xóa đối tác ${c?.name} không?`)) return;

  try {
    const res = await authFetch(`/api/customers/${id}`, { method: 'DELETE' });
    if (res.ok) {
      showToast("Đã xóa khách hàng!");
      loadCustomers();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi xóa khách hàng", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

// ==========================================
// 4. BIDDING PIPELINE KANBAN LOGIC
// ==========================================
async function loadPipeline() {
  try {
    const sbuParam = currentSBU !== 'ALL' ? `?sbu=${currentSBU}` : '';
    const res = await authFetch(`/api/customers/bids/pipeline${sbuParam}`);
    const data = await res.json();
    window.allBidsList = data.items ? Object.values(data.items).flat() : [];

    const board = document.getElementById('pipeline-board');
    if (!board) return;

    board.innerHTML = data.stages.map(stage => {
      const items = data.items[stage.id] || [];
      const totalVal = items.reduce((sum, item) => sum + (item.estimated_value || 0), 0);

      return `
        <div id="kanban-col-${stage.id}" class="kanban-column bg-slate-100 rounded-2xl p-3 flex flex-col border border-slate-200 transition-all duration-500">
          <div class="flex items-center justify-between pb-2 mb-2 border-b border-slate-200">
            <div>
              <div class="font-black text-xs text-slate-900">${stage.label}</div>
              <div class="text-[10px] text-purple-700 font-bold">${formatVND(totalVal)}</div>
            </div>
            <span class="w-5 h-5 rounded-full bg-white text-slate-800 font-black text-[11px] flex items-center justify-center shadow-xs">
              ${items.length}
            </span>
          </div>

          <div class="flex-1 space-y-2.5 overflow-y-auto max-h-[600px] pr-1">
            ${items.length > 0 ? items.map(item => {
              const canEditBid = currentUser.role === 'ADMIN' || currentUser.sbu === item.sbu;

              const cls = classifyFeconProject(item.estimated_value);

              return `
                <div onclick="openBidDetailModal(${item.id})" class="bg-white p-3 rounded-xl border border-slate-200 shadow-xs hover-card space-y-2 cursor-pointer hover:border-purple-400 transition" title="Bấm để xem chi tiết & cập nhật trạng thái gói thầu">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-1.5">
                      ${getSBUBadge(item.sbu)}
                      <span class="px-1.5 py-0.5 rounded text-[9px] font-black border ${cls.colorClass}">
                        ${cls.badge}
                      </span>
                    </div>
                    <span class="text-[10px] font-black text-purple-700">${item.win_rate}% Win</span>
                  </div>
                  <div class="font-extrabold text-slate-900 text-xs leading-snug">${item.project_title}</div>
                  <div class="text-[11px] text-slate-500">${item.customer_name}</div>
                  <div class="text-[10px] text-indigo-900 font-medium">
                    Duyệt CSKH: <b class="font-bold">${cls.approver_short}</b>
                  </div>
                  <div class="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
                    <span class="font-black text-slate-900">${formatVND(item.estimated_value)}</span>
                    <div class="flex items-center gap-1" onclick="event.stopPropagation()">
                      ${canEditBid && stage.id !== 'WON' && stage.id !== 'LOST' ? `
                        <button onclick="advanceBidStage(${item.id}, '${stage.id}')" class="px-2 py-0.5 bg-purple-50 hover:bg-purple-100 text-purple-700 rounded text-[10px] font-bold">
                          Tiến &gt;
                        </button>
                      ` : ''}
                      ${currentUser.role === 'ADMIN' ? `
                        <button onclick="deleteBid(${item.id})" class="p-1 text-slate-400 hover:text-rose-600 text-[10px]" title="Xóa thầu">
                          <i class="fa-solid fa-trash"></i>
                        </button>
                      ` : ''}
                    </div>
                  </div>
                </div>
              `;
            }).join('') : '<div class="text-[11px] text-slate-400 text-center py-6">Không có hồ sơ</div>'}
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    console.error("Error loading pipeline:", err);
  }
}

async function openBidDetailModal(bidId) {
  try {
    const res = await authFetch(`/api/customers/bids/${bidId}`);
    if (!res.ok) {
      showToast("Không tìm thấy thông tin gói thầu", "error");
      return;
    }
    const bid = await res.json();

    document.getElementById('bid-detail-id').value = bid.id;
    document.getElementById('bid-detail-title').textContent = bid.project_title;
    document.getElementById('bid-detail-customer').textContent = bid.customer_name || '--';
    document.getElementById('bid-detail-contact').textContent = `${bid.key_decision_maker || 'Chưa cập nhật'} (${bid.decision_maker_phone || '--'})`;
    document.getElementById('bid-detail-value').textContent = formatVND(bid.estimated_value);
    document.getElementById('bid-detail-deadline').textContent = bid.tender_deadline || 'Chưa thiết lập';
    document.getElementById('bid-detail-director').textContent = bid.assigned_director || 'Chưa chỉ định';
    
    const cls = classifyFeconProject(bid.estimated_value);
    const lvlBadge = document.getElementById('bid-detail-level-badge');
    const approverEl = document.getElementById('bid-detail-approver');
    if (lvlBadge) {
      lvlBadge.textContent = cls.badge;
      lvlBadge.className = `px-2 py-0.5 rounded-full font-bold text-[10px] border ${cls.colorClass}`;
    }
    if (approverEl) {
      approverEl.textContent = cls.approver_authority;
    }

    const sbuBadge = document.getElementById('bid-detail-sbu-badge');
    if (sbuBadge) {
      sbuBadge.textContent = bid.sbu;
      sbuBadge.className = `px-2 py-0.5 rounded text-[10px] font-black uppercase ${bid.sbu === 'SBU1' ? 'bg-blue-100 text-blue-800' : 'bg-purple-100 text-purple-800'}`;
    }

    const stageSelect = document.getElementById('bid-detail-stage');
    if (stageSelect) stageSelect.value = bid.stage || 'INFORMATION';

    const winrateInput = document.getElementById('bid-detail-winrate');
    if (winrateInput) winrateInput.value = (bid.win_rate !== undefined && bid.win_rate !== null) ? bid.win_rate : 50;

    const notesInput = document.getElementById('bid-detail-notes');
    if (notesInput) notesInput.value = bid.bidding_notes || '';

    const delBtn = document.getElementById('btn-delete-bid-modal');
    if (delBtn) delBtn.style.display = currentUser.role === 'ADMIN' ? 'flex' : 'none';

    openModal('modal-bid-detail');
  } catch (err) {
    showToast("Lỗi khi mở chi tiết gói thầu", "error");
  }
}

async function quickChangeBidStage(targetStage) {
  const bidId = document.getElementById('bid-detail-id').value;
  if (!bidId) return;

  const stageSelect = document.getElementById('bid-detail-stage');
  if (stageSelect) stageSelect.value = targetStage;

  const winrateInput = document.getElementById('bid-detail-winrate');
  if (winrateInput) {
    if (targetStage === 'WON') winrateInput.value = 100;
    else if (targetStage === 'LOST') winrateInput.value = 0;
  }

  await saveBidDetailChanges();
}

async function saveBidDetailChanges() {
  const bidId = document.getElementById('bid-detail-id').value;
  if (!bidId) return;

  const stage = document.getElementById('bid-detail-stage').value;
  const winRate = parseInt(document.getElementById('bid-detail-winrate').value, 10) || 50;
  const notes = document.getElementById('bid-detail-notes').value.trim();

  try {
    const res = await authFetch(`/api/customers/bids/${bidId}/stage`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        stage: stage,
        win_rate: winRate,
        bidding_notes: notes
      })
    });

    if (res.ok) {
      const stageName = stage === 'WON' ? '🏆 TRÚNG THẦU' : (stage === 'LOST' ? '❌ TRƯỢT THẦU' : stage);
      showToast(`Đã cập nhật trạng thái gói thầu: ${stageName}!`);
      closeModal('modal-bid-detail');
      loadPipeline();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi khi lưu", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối máy chủ", "error");
  }
}

async function deleteCurrentBid() {
  const bidId = document.getElementById('bid-detail-id').value;
  if (!bidId) return;
  await deleteBid(bidId);
  closeModal('modal-bid-detail');
}

async function advanceBidStage(bidId, currentStage) {
  const stageOrder = ["INFORMATION", "EVALUATION", "TENDER_PREP", "NEGOTIATION", "WON"];
  const curIdx = stageOrder.indexOf(currentStage);
  if (curIdx < 0 || curIdx >= stageOrder.length - 1) return;

  const nextStage = stageOrder[curIdx + 1];
  try {
    const res = await authFetch(`/api/customers/bids/${bidId}/stage`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ stage: nextStage })
    });
    if (res.ok) {
      showToast("Đã chuyển giai đoạn đấu thầu thành công!");
      loadPipeline();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi cập nhật", "error");
    }
  } catch (err) {
    showToast("Lỗi khi chuyển giai đoạn", "error");
  }
}

async function deleteBid(bidId) {
  if (currentUser.role !== 'ADMIN') return;
  if (!confirm("Xóa hồ sơ dự thầu này khỏi hệ thống?")) return;

  try {
    const res = await authFetch(`/api/customers/bids/${bidId}`, { method: 'DELETE' });
    if (res.ok) {
      showToast("Đã xóa hồ sơ thầu!");
      loadPipeline();
      loadDashboard();
    }
  } catch (e) {
    showToast("Lỗi xóa hồ sơ thầu", "error");
  }
}

function populateBidCustomerSelect(selectedId = null) {
  const custSelect = document.getElementById('bid-customer');
  if (!custSelect) return;
  let opts = '<option value="">-- Chọn Chủ đầu tư / Đối tác có sẵn --</option>';
  opts += allCustomers.map(c => `<option value="${c.id}">${c.name} (${c.sbu})</option>`).join('');
  opts += '<option value="__NEW__" class="font-bold text-blue-600 bg-blue-50">➕ Thêm Chủ đầu tư / Khách hàng mới...</option>';
  custSelect.innerHTML = opts;
  if (selectedId) {
    custSelect.value = selectedId;
  }
}

function toggleQuickAddBidCustomer(forceOpen = null) {
  const box = document.getElementById('box-quick-add-bid-customer');
  if (!box) return;
  const isOpen = !box.classList.contains('hidden');
  const shouldOpen = forceOpen !== null ? forceOpen : !isOpen;
  if (shouldOpen) {
    box.classList.remove('hidden');
    document.getElementById('quick-bid-cust-name')?.focus();
    const custSelect = document.getElementById('bid-customer');
    if (custSelect && custSelect.value !== '__NEW__') {
      custSelect.value = '__NEW__';
    }
  } else {
    box.classList.add('hidden');
    const custSelect = document.getElementById('bid-customer');
    if (custSelect && custSelect.value === '__NEW__') {
      custSelect.value = '';
    }
  }
}

function handleBidCustomerChange(val) {
  if (val === '__NEW__') {
    toggleQuickAddBidCustomer(true);
  } else {
    toggleQuickAddBidCustomer(false);
  }
}

async function submitQuickBidCustomer() {
  const name = document.getElementById('quick-bid-cust-name')?.value.trim();
  const contact = document.getElementById('quick-bid-cust-contact')?.value.trim();
  const phone = document.getElementById('quick-bid-cust-phone')?.value.trim() || '0900000000';
  const segment = document.getElementById('quick-bid-cust-segment')?.value || 'B2B';
  const tier = document.getElementById('quick-bid-cust-tier')?.value || 'GOLD';
  const sbuSelect = document.getElementById('bid-sbu');
  const sbu = currentUser.role === 'SBU_DIRECTOR' ? currentUser.sbu : (sbuSelect?.value || 'SBU1');

  if (!name) {
    showToast("Vui lòng nhập tên Chủ đầu tư / Doanh nghiệp!", "error");
    document.getElementById('quick-bid-cust-name')?.focus();
    return null;
  }
  if (!contact) {
    showToast("Vui lòng nhập tên Lãnh đạo / Người liên hệ!", "error");
    document.getElementById('quick-bid-cust-contact')?.focus();
    return null;
  }

  const payload = {
    name: name,
    sbu: sbu,
    key_decision_maker: contact,
    decision_maker_phone: phone,
    phone: phone,
    tier: tier,
    segment: segment
  };

  try {
    const res = await authFetch('/api/customers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      showToast(err.detail || "Lỗi khi tạo Chủ đầu tư mới", "error");
      return null;
    }

    const newCust = await res.json();
    showToast(`Đã thêm CĐT "${newCust.name}" thành công!`);

    await loadCustomers();
    populateBidCustomerSelect(newCust.id);
    toggleQuickAddBidCustomer(false);

    document.getElementById('quick-bid-cust-name').value = '';
    document.getElementById('quick-bid-cust-contact').value = '';
    document.getElementById('quick-bid-cust-phone').value = '';

    return newCust;
  } catch (err) {
    console.error("Error creating quick customer for bid:", err);
    showToast("Lỗi kết nối khi tạo Chủ đầu tư", "error");
    return null;
  }
}

function openNewBidModal() {
  document.getElementById('form-bid').reset();
  toggleQuickAddBidCustomer(false);
  populateBidCustomerSelect();
  const sbuSelect = document.getElementById('bid-sbu');
  if (currentUser.role === 'SBU_DIRECTOR') {
    sbuSelect.value = currentUser.sbu;
    sbuSelect.disabled = true;
  } else {
    sbuSelect.disabled = false;
    if (currentSBU !== 'ALL') sbuSelect.value = currentSBU;
  }
  updateBidLevelPreview();
  openModal('modal-bid');
}

async function handleBidSubmit(e) {
  e.preventDefault();

  let custId = document.getElementById('bid-customer').value;
  // If user selected __NEW__ or entered quick customer name without clicking the check button:
  if (custId === '__NEW__' || (!custId && document.getElementById('quick-bid-cust-name')?.value.trim())) {
    const created = await submitQuickBidCustomer();
    if (!created) return;
    custId = created.id;
  }

  if (!custId) {
    showToast("Vui lòng chọn hoặc thêm Chủ đầu tư cho gói thầu!", "error");
    return;
  }

  const sbuSelect = document.getElementById('bid-sbu');
  const payload = {
    customer_id: parseInt(custId, 10),
    sbu: currentUser.role === 'SBU_DIRECTOR' ? currentUser.sbu : sbuSelect.value,
    project_title: document.getElementById('bid-title').value.trim(),
    estimated_value: parseFloat(document.getElementById('bid-val').value) || 0,
    stage: document.getElementById('bid-stage').value,
    win_rate: parseInt(document.getElementById('bid-winrate').value, 10) || 50,
    tender_deadline: document.getElementById('bid-deadline').value
  };

  try {
    const res = await authFetch('/api/customers/bids', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast("Thêm gói thầu / cơ hội thành công!");
      closeModal('modal-bid');
      loadPipeline();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi thêm gói thầu", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

// ==========================================
// 5. PROJECTS & CASHFLOW TRACKING LOGIC
// ==========================================
function classifyFeconProject(val) {
  const v = parseFloat(val) || 0;
  if (v >= 500000000000) {
    return {
      level: "LEVEL_SPECIAL",
      code: "ĐẶC BIỆT",
      name: "Dự án Cấp Đặc Biệt",
      badge: "Cấp Đặc Biệt",
      approver_authority: "Chủ tịch HĐQT quyết định",
      approver_short: "Chủ tịch HĐQT",
      colorClass: "bg-purple-100 text-purple-800 border-purple-200",
      accentBg: "bg-purple-600 text-white"
    };
  } else if (v >= 300000000000) {
    return {
      level: "LEVEL_1",
      code: "CẤP 1",
      name: "Dự án Cấp 1",
      badge: "Cấp 1",
      approver_authority: "Tổng Giám đốc (hoặc PTGĐ có ủy quyền của Chủ tịch)",
      approver_short: "Tổng Giám đốc (hoặc PTGĐ ủy quyền)",
      colorClass: "bg-rose-100 text-rose-800 border-rose-200",
      accentBg: "bg-rose-600 text-white"
    };
  } else if (v >= 150000000000) {
    return {
      level: "LEVEL_2",
      code: "CẤP 2",
      name: "Dự án Cấp 2",
      badge: "Cấp 2",
      approver_authority: "Phó Tổng Giám đốc phụ trách các mảng SBU",
      approver_short: "PTGĐ phụ trách SBU",
      colorClass: "bg-amber-100 text-amber-900 border-amber-200",
      accentBg: "bg-amber-500 text-slate-950"
    };
  } else if (v >= 50000000000) {
    return {
      level: "LEVEL_3",
      code: "CẤP 3",
      name: "Dự án Cấp 3",
      badge: "Cấp 3",
      approver_authority: "Phó Tổng Giám đốc phụ trách các mảng SBU",
      approver_short: "PTGĐ phụ trách SBU",
      colorClass: "bg-blue-100 text-blue-800 border-blue-200",
      accentBg: "bg-blue-600 text-white"
    };
  } else {
    return {
      level: "LEVEL_4",
      code: "CẤP 4",
      name: "Dự án Cấp 4",
      badge: "Cấp 4",
      approver_authority: "Phó Tổng Giám đốc phụ trách các mảng SBU",
      approver_short: "PTGĐ phụ trách SBU",
      colorClass: "bg-emerald-100 text-emerald-800 border-emerald-200",
      accentBg: "bg-emerald-600 text-white"
    };
  }
}

function updateProjectLevelPreview() {
  const val = parseFloat(document.getElementById('proj-val')?.value) || 0;
  const cls = classifyFeconProject(val);
  const badgeEl = document.getElementById('proj-level-badge');
  const approverEl = document.getElementById('proj-approver-text');
  if (badgeEl) {
    badgeEl.textContent = cls.badge;
    badgeEl.className = `px-2 py-0.5 rounded-full font-bold border ${cls.colorClass}`;
  }
  if (approverEl) {
    approverEl.textContent = cls.approver_authority;
  }
}

function updateBidLevelPreview() {
  const val = parseFloat(document.getElementById('bid-val')?.value) || 0;
  const cls = classifyFeconProject(val);
  const badgeEl = document.getElementById('bid-level-badge');
  const approverEl = document.getElementById('bid-approver-text');
  if (badgeEl) {
    badgeEl.textContent = cls.badge;
    badgeEl.className = `px-2 py-0.5 rounded-full font-bold border ${cls.colorClass}`;
  }
  if (approverEl) {
    approverEl.textContent = cls.approver_authority;
  }
}

let currentProjectLevelFilter = 'ALL';

function filterProjectsByLevel(lvl) {
  currentProjectLevelFilter = lvl;
  document.querySelectorAll('.btn-proj-filter').forEach(btn => {
    if (btn.getAttribute('data-level') === lvl) {
      btn.className = "btn-proj-filter px-3 py-1.5 rounded-xl font-bold bg-[#0A3583] text-white shadow-xs";
    } else {
      btn.className = "btn-proj-filter px-3 py-1.5 rounded-xl font-bold bg-white text-slate-700 border border-slate-200 hover:bg-slate-50";
    }
  });
  renderProjectsList();
}

async function loadProjects() {
  try {
    const sbuParam = currentSBU !== 'ALL' ? `?sbu=${currentSBU}` : '';
    const res = await authFetch(`/api/projects${sbuParam}`);
    allProjects = await res.json();
    renderProjectsList();
  } catch (err) {
    console.error("Error loading projects:", err);
  }
}

function renderProjectsList() {
  const container = document.getElementById('projects-container');
  if (!container) return;

  const filtered = allProjects.filter(p => {
    if (currentProjectLevelFilter === 'ALL') return true;
    const cls = classifyFeconProject(p.contract_value);
    const pLevel = p.project_level || cls.level;
    return pLevel === currentProjectLevelFilter;
  });

  if (filtered.length === 0) {
    container.innerHTML = '<div class="col-span-2 text-center text-slate-400 text-xs py-8 bg-white rounded-2xl border border-slate-200">Không có dự án nào phù hợp với bộ lọc cấp này.</div>';
    return;
  }

  container.innerHTML = filtered.map(p => {
    const canEdit = currentUser.role === 'ADMIN' || (currentUser.role === 'SBU_DIRECTOR' && currentUser.sbu === p.sbu);
    const canDelete = currentUser.role === 'ADMIN';
    const cls = classifyFeconProject(p.contract_value);

    return `
      <div onclick="viewProjectDetail(${p.id})" class="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm hover-card space-y-3 cursor-pointer hover:border-blue-400 transition">
        <div class="flex items-start justify-between gap-2">
          <div>
            <div class="flex items-center gap-1.5 flex-wrap">
              ${getSBUBadge(p.sbu)}
              <span class="px-2 py-0.5 rounded-full text-[10px] font-black border ${cls.colorClass}">
                ${cls.badge}
              </span>
              <span class="text-xs font-mono font-bold text-slate-400">${p.code}</span>
            </div>
            <h3 class="font-black text-slate-900 text-sm mt-1.5">${p.name}</h3>
            <p class="text-xs text-slate-500">${p.customer_name} • ${p.key_decision_maker || ''}</p>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${p.project_health === 'GOOD' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}">
              ${p.project_health === 'GOOD' ? '🟢 Tiến độ tốt' : '🟡 Cần lưu ý'}
            </span>
            ${canDelete ? `
              <button onclick="event.stopPropagation(); deleteProject(${p.id})" class="p-1 text-slate-300 hover:text-rose-600 transition" title="Xóa dự án (Admin)">
                <i class="fa-solid fa-trash"></i>
              </button>
            ` : ''}
          </div>
        </div>

        <div>
          <div class="flex justify-between text-xs font-bold mb-1">
            <span class="text-slate-600">Tiến độ thi công cam kết:</span>
            <span class="text-emerald-600">${p.progress_percent}%</span>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
            <div class="bg-emerald-500 h-2.5 rounded-full" style="width: ${p.progress_percent}%"></div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-2 p-3 bg-slate-50 rounded-xl text-xs">
          <div>
            <div class="text-[10px] uppercase font-bold text-slate-400">Tổng Giá Trị Hợp Đồng</div>
            <div class="font-black text-slate-900">${formatVND(p.contract_value)}</div>
          </div>
          <div>
            <div class="text-[10px] uppercase font-bold text-slate-400">Đã Giải Ngân Thực Tế</div>
            <div class="font-black text-emerald-600">${formatVND(p.paid_amount)}</div>
          </div>
        </div>

        <!-- Approval Authority Indicator -->
        <div class="p-2.5 bg-blue-50/70 rounded-xl border border-blue-200/60 flex items-center justify-between text-[11px]">
          <div class="text-slate-600 font-medium">
            <i class="fa-solid fa-stamp text-blue-600 mr-1"></i> Thẩm quyền duyệt chi phí CSKH:
          </div>
          <div class="font-extrabold text-blue-950 text-right">
            ${p.approver_short || cls.approver_short}
          </div>
        </div>

        <div class="flex items-center justify-between text-[11px] pt-1 text-slate-500">
          <div>GĐKD phụ trách: <span class="font-bold text-slate-700">${p.project_director || 'Đang cập nhật'}</span></div>
          <button onclick="event.stopPropagation(); viewProjectDetail(${p.id})" class="text-blue-600 font-bold hover:underline">
            Xem Chi Tiết & Mốc Dòng Tiền &gt;
          </button>
        </div>
      </div>
    `;
  }).join('');
}

let currentViewProjectId = null;

async function viewProjectDetail(id) {
  try {
    currentViewProjectId = id;
    const res = await authFetch(`/api/projects/${id}`);
    if (!res.ok) {
      showToast("Không tìm thấy thông tin dự án", "error");
      return;
    }
    const p = await res.json();
    const cls = classifyFeconProject(p.contract_value);
    const canEdit = currentUser.role === 'ADMIN' || (currentUser.role === 'SBU_DIRECTOR' && currentUser.sbu === p.sbu);

    // SBU Badge
    const sbuContainer = document.getElementById('proj-detail-sbu-container');
    if (sbuContainer) {
      sbuContainer.innerHTML = getSBUBadge(p.sbu);
    } else {
      const sbuBadge = document.getElementById('proj-detail-sbu-badge');
      if (sbuBadge) sbuBadge.outerHTML = `<span id="proj-detail-sbu-container">${getSBUBadge(p.sbu)}</span>`;
    }

    // Level Badge
    const lvlBadge = document.getElementById('proj-detail-level-badge');
    if (lvlBadge) {
      lvlBadge.textContent = cls.badge;
      lvlBadge.className = `px-2.5 py-0.5 rounded-full text-[10px] font-black border ${cls.colorClass}`;
    }

    // Code & Titles
    const codeEl = document.getElementById('proj-detail-code');
    if (codeEl) codeEl.textContent = p.code || `DA-${p.id}`;

    const nameEl = document.getElementById('proj-detail-name');
    if (nameEl) nameEl.textContent = p.name;

    const subEl = document.getElementById('proj-detail-subtitle');
    if (subEl) subEl.textContent = `Chủ đầu tư: ${p.customer_name || 'N/A'} • HĐ: ${p.contract_number || 'Chưa có số HĐ'}`;

    // Hero box classification & approver
    const clsBox = document.getElementById('proj-detail-classification-box');
    if (clsBox) {
      let boxBg = 'bg-purple-50/70 border-purple-200/80';
      if (cls.level === 'LEVEL_1') boxBg = 'bg-rose-50/70 border-rose-200/80';
      else if (cls.level === 'LEVEL_2') boxBg = 'bg-amber-50/70 border-amber-200/80';
      else if (cls.level === 'LEVEL_3') boxBg = 'bg-blue-50/70 border-blue-200/80';
      else if (cls.level === 'LEVEL_4') boxBg = 'bg-emerald-50/70 border-emerald-200/80';
      clsBox.className = `p-4 rounded-xl border space-y-2 ${boxBg}`;
    }

    const lvlTag = document.getElementById('proj-detail-level-tag');
    if (lvlTag) {
      lvlTag.textContent = cls.name.toUpperCase();
      lvlTag.className = `px-2 py-0.5 rounded-md font-black text-xs ${cls.accentBg}`;
    }

    const valHead = document.getElementById('proj-detail-value-head');
    if (valHead) valHead.textContent = formatVND(p.contract_value);

    const approverEl = document.getElementById('proj-detail-approver');
    if (approverEl) approverEl.textContent = p.approver_authority || cls.approver_authority;

    // Partner & Contacts
    const custEl = document.getElementById('proj-detail-customer');
    if (custEl) custEl.textContent = p.customer_name || 'Đang cập nhật';

    const contactEl = document.getElementById('proj-detail-contact');
    if (contactEl) contactEl.textContent = p.key_decision_maker || 'Chưa cập nhật';

    const phoneEl = document.getElementById('proj-detail-phone');
    if (phoneEl) {
      if (p.customer_phone) {
        phoneEl.innerHTML = `<a href="tel:${p.customer_phone}" class="text-blue-600 hover:underline font-bold"><i class="fa-solid fa-phone mr-1"></i>${p.customer_phone}</a>`;
      } else {
        phoneEl.textContent = 'Chưa có SĐT';
      }
    }

    const contractEl = document.getElementById('proj-detail-contract-no');
    if (contractEl) contractEl.textContent = p.contract_number || 'Chưa có số HĐ';

    const directorEl = document.getElementById('proj-detail-director');
    if (directorEl) directorEl.textContent = p.project_director || 'Chưa phân công';

    // Progress & Finance
    const progress = p.progress_percent || 0;
    const progPctEl = document.getElementById('proj-detail-progress-pct');
    if (progPctEl) progPctEl.textContent = `${progress}%`;

    const progBar = document.getElementById('proj-detail-progress-bar');
    if (progBar) progBar.style.width = `${progress}%`;

    const valEl = document.getElementById('proj-detail-val');
    if (valEl) valEl.textContent = formatVND(p.contract_value);

    const paidEl = document.getElementById('proj-detail-paid');
    if (paidEl) paidEl.textContent = formatVND(p.paid_amount || 0);

    const unpaid = Math.max(0, (p.contract_value || 0) - (p.paid_amount || 0));
    const unpaidEl = document.getElementById('proj-detail-unpaid');
    if (unpaidEl) unpaidEl.textContent = formatVND(unpaid);

    const healthEl = document.getElementById('proj-detail-health');
    if (healthEl) {
      healthEl.innerHTML = p.project_health === 'GOOD'
        ? '<span class="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-bold">🟢 Tiến độ tốt</span>'
        : '<span class="text-amber-700 bg-amber-50 px-2 py-0.5 rounded font-bold">🟡 Cần lưu ý</span>';
    }

    // Scope if any
    const scopeBox = document.getElementById('proj-detail-scope-box');
    const scopeEl = document.getElementById('proj-detail-scope');
    if (scopeBox && scopeEl) {
      if (p.scope_of_work) {
        scopeEl.textContent = p.scope_of_work;
        scopeBox.classList.remove('hidden');
      } else {
        scopeBox.classList.add('hidden');
      }
    }

    // Milestones
    const countEl = document.getElementById('proj-detail-milestones-count');
    if (countEl) countEl.textContent = `${p.milestones ? p.milestones.length : 0} mốc`;

    const mList = document.getElementById('proj-detail-milestones-list');
    if (mList) {
      if (p.milestones && p.milestones.length > 0) {
        mList.innerHTML = p.milestones.map(m => `
          <div class="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div class="font-bold text-slate-900 text-xs truncate">${m.title}</div>
              <div class="text-[10px] text-slate-500 mt-0.5">
                Hạn TT: <span class="font-medium text-slate-700">${m.due_date || 'N/A'}</span> • Tỷ lệ: <span class="font-bold text-blue-600">${m.percentage}%</span>
              </div>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <span class="font-black text-xs text-slate-900">${formatVND(m.amount)}</span>
              <span class="px-2 py-0.5 rounded text-[10px] font-bold ${m.payment_status === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}">
                ${m.payment_status === 'PAID' ? 'Đã thu' : 'Chờ thu'}
              </span>
              ${canEdit && m.payment_status !== 'PAID' ? `
                <button onclick="confirmDisbursement(${m.id})" class="px-2 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[10px] font-bold cursor-pointer transition shadow-xs">
                  Xác nhận đã thu
                </button>
              ` : ''}
            </div>
          </div>
        `).join('');
      } else {
        mList.innerHTML = '<div class="text-slate-400 text-xs py-4 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">Chưa có mốc nghiệm thu nào được tạo.</div>';
      }
    }

    // Admin delete button
    const delBtn = document.getElementById('btn-delete-project-modal');
    if (delBtn) {
      if (currentUser.role === 'ADMIN') {
        delBtn.classList.remove('hidden');
      } else {
        delBtn.classList.add('hidden');
      }
    }

    openModal('modal-project-detail');
  } catch (err) {
    console.error("Error viewing project detail:", err);
    showToast("Lỗi khi mở chi tiết dự án", "error");
  }
}

function viewProjectCashflow(id) {
  viewProjectDetail(id);
}

async function deleteCurrentProject() {
  if (!currentViewProjectId) return;
  const id = currentViewProjectId;
  closeModal('modal-project-detail');
  await deleteProject(id);
}

async function confirmDisbursement(milestoneId) {
  try {
    const res = await authFetch(`/api/projects/milestones/${milestoneId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ payment_status: 'PAID' })
    });
    if (res.ok) {
      showToast("Đã xác nhận thu hồi dòng tiền thành công!");
      if (currentViewProjectId) {
        viewProjectDetail(currentViewProjectId);
      }
      closeModal('modal-customer-detail');
      loadProjects();
      loadDashboard();
    }
  } catch (err) {
    showToast("Lỗi khi xác nhận", "error");
  }
}

function populateProjectCustomerSelect(selectedId = null) {
  const custSelect = document.getElementById('proj-customer');
  if (!custSelect) return;
  let opts = '<option value="">-- Chọn Chủ đầu tư / Đối tác có sẵn --</option>';
  opts += allCustomers.map(c => `<option value="${c.id}">${c.name} (${c.sbu})</option>`).join('');
  opts += '<option value="__NEW__" class="font-bold text-blue-600 bg-blue-50">➕ Thêm Chủ đầu tư / Khách hàng mới...</option>';
  custSelect.innerHTML = opts;
  if (selectedId) {
    custSelect.value = selectedId;
  }
}

function toggleQuickAddCustomer(forceOpen = null) {
  const box = document.getElementById('box-quick-add-customer');
  if (!box) return;
  const isOpen = !box.classList.contains('hidden');
  const shouldOpen = forceOpen !== null ? forceOpen : !isOpen;
  if (shouldOpen) {
    box.classList.remove('hidden');
    document.getElementById('quick-cust-name')?.focus();
    const custSelect = document.getElementById('proj-customer');
    if (custSelect && custSelect.value !== '__NEW__') {
      custSelect.value = '__NEW__';
    }
  } else {
    box.classList.add('hidden');
    const custSelect = document.getElementById('proj-customer');
    if (custSelect && custSelect.value === '__NEW__') {
      custSelect.value = '';
    }
  }
}

function handleProjectCustomerChange(val) {
  if (val === '__NEW__') {
    toggleQuickAddCustomer(true);
  } else {
    toggleQuickAddCustomer(false);
  }
}

async function submitQuickCustomer() {
  const name = document.getElementById('quick-cust-name')?.value.trim();
  const contact = document.getElementById('quick-cust-contact')?.value.trim();
  const phone = document.getElementById('quick-cust-phone')?.value.trim() || '0900000000';
  const segment = document.getElementById('quick-cust-segment')?.value || 'B2B';
  const tier = document.getElementById('quick-cust-tier')?.value || 'GOLD';
  const sbuSelect = document.getElementById('proj-sbu');
  const sbu = currentUser.role === 'SBU_DIRECTOR' ? currentUser.sbu : (sbuSelect?.value || 'SBU1');

  if (!name) {
    showToast("Vui lòng nhập tên Chủ đầu tư / Doanh nghiệp!", "error");
    document.getElementById('quick-cust-name')?.focus();
    return null;
  }
  if (!contact) {
    showToast("Vui lòng nhập tên Lãnh đạo / Người liên hệ!", "error");
    document.getElementById('quick-cust-contact')?.focus();
    return null;
  }

  const payload = {
    name: name,
    sbu: sbu,
    key_decision_maker: contact,
    decision_maker_phone: phone,
    phone: phone,
    tier: tier,
    segment: segment
  };

  try {
    const res = await authFetch('/api/customers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      showToast(err.detail || "Lỗi khi tạo Chủ đầu tư mới", "error");
      return null;
    }

    const newCust = await res.json();
    showToast(`Đã thêm CĐT "${newCust.name}" thành công!`);

    // Refresh allCustomers and update select
    await loadCustomers();
    populateProjectCustomerSelect(newCust.id);
    toggleQuickAddCustomer(false);

    // Reset quick fields
    document.getElementById('quick-cust-name').value = '';
    document.getElementById('quick-cust-contact').value = '';
    document.getElementById('quick-cust-phone').value = '';

    return newCust;
  } catch (err) {
    console.error("Error creating quick customer:", err);
    showToast("Lỗi kết nối khi tạo Chủ đầu tư", "error");
    return null;
  }
}

function openNewProjectModal() {
  document.getElementById('form-project').reset();
  toggleQuickAddCustomer(false);
  populateProjectCustomerSelect();
  const sbuSelect = document.getElementById('proj-sbu');
  if (currentUser.role === 'SBU_DIRECTOR') {
    sbuSelect.value = currentUser.sbu;
    sbuSelect.disabled = true;
    document.getElementById('proj-director').value = currentUser.full_name;
  } else {
    sbuSelect.disabled = false;
    if (currentSBU !== 'ALL') sbuSelect.value = currentSBU;
  }
  updateProjectLevelPreview();
  openModal('modal-project');
}

async function handleProjectSubmit(e) {
  e.preventDefault();

  let custId = document.getElementById('proj-customer').value;
  // If user selected __NEW__ or entered quick customer name without clicking the check button:
  if (custId === '__NEW__' || (!custId && document.getElementById('quick-cust-name')?.value.trim())) {
    const created = await submitQuickCustomer();
    if (!created) return;
    custId = created.id;
  }

  if (!custId) {
    showToast("Vui lòng chọn hoặc thêm Chủ đầu tư cho dự án!", "error");
    return;
  }

  const sbuSelect = document.getElementById('proj-sbu');
  const payload = {
    code: document.getElementById('proj-code').value.trim(),
    name: document.getElementById('proj-name').value.trim(),
    customer_id: parseInt(custId, 10),
    sbu: currentUser.role === 'SBU_DIRECTOR' ? currentUser.sbu : sbuSelect.value,
    contract_value: parseFloat(document.getElementById('proj-val').value) || 0,
    contract_number: document.getElementById('proj-contract').value.trim(),
    project_director: document.getElementById('proj-director').value.trim()
  };

  try {
    const res = await authFetch('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast("Khởi tạo dự án theo dõi thành công!");
      closeModal('modal-project');
      loadProjects();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi khởi tạo dự án", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

async function deleteProject(id) {
  if (currentUser.role !== 'ADMIN') {
    showToast("Chỉ Ban Lãnh Đạo mới có quyền xóa dự án!", "error");
    return;
  }

  const p = allProjects.find(x => x.id === id);
  if (!confirm(`Xóa dự án "${p?.name}" khỏi hệ thống theo dõi?`)) return;

  try {
    const res = await authFetch(`/api/projects/${id}`, { method: 'DELETE' });
    if (res.ok) {
      showToast("Đã xóa dự án!");
      loadProjects();
      loadDashboard();
    } else {
      const err = await res.json();
      showToast(err.detail || "Lỗi khi xóa", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

// ==========================================
// 6. EXECUTIVE CARE & NETWORKING LOGIC
// ==========================================
async function loadCareActivities() {
  try {
    const sbuParam = currentSBU !== 'ALL' ? `?sbu=${currentSBU}` : '';
    const res = await authFetch(`/api/care-activities${sbuParam}`);
    const activities = await res.json();

    const container = document.getElementById('care-activities-container');
    if (!container) return;

    if (activities.length === 0) {
      container.innerHTML = '<div class="text-center text-slate-400 text-xs py-6">Chưa có nhật ký tiếp khách ngoại giao.</div>';
      return;
    }

    const typeLabels = {
      "DINNER_NETWORKING": "Bữa tối thân mật",
      "EXECUTIVE_MEETING": "Họp chiến lược cấp cao",
      "GIFT_DELIVERY": "Gửi quà tri ân",
      "EVENT_INVITATION": "Mời dự sự kiện",
      "CALL_DISCUSS": "Điện đàm ngoại giao"
    };

    container.innerHTML = activities.map(a => {
      let levelBadge = '';
      if (a.project_level === 'LEVEL_SPECIAL' || (a.project_contract_value >= 500000000000)) {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-black bg-purple-100 text-purple-800 border border-purple-200">Cấp Đặc Biệt</span>`;
      } else if (a.project_level === 'LEVEL_1' || (a.project_contract_value >= 300000000000)) {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-black bg-rose-100 text-rose-800 border border-rose-200">Cấp 1</span>`;
      } else if (a.project_level === 'LEVEL_2' || (a.project_contract_value >= 150000000000)) {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-black bg-amber-100 text-amber-900 border border-amber-200">Cấp 2</span>`;
      } else if (a.project_level === 'LEVEL_3' || (a.project_contract_value >= 50000000000)) {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-black bg-blue-100 text-blue-800 border border-blue-200">Cấp 3</span>`;
      } else if (a.project_level === 'LEVEL_4' || (a.project_contract_value > 0)) {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-black bg-emerald-100 text-emerald-800 border border-emerald-200">Cấp 4</span>`;
      } else {
        levelBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">CSKH Chung</span>`;
      }

      return `
        <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200/80 space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-1.5 flex-wrap">
              ${getSBUBadge(a.sbu)}
              ${levelBadge}
              <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-50 text-indigo-700">${typeLabels[a.activity_type] || a.activity_type}</span>
              <span class="font-extrabold text-slate-900 text-xs">${a.title}</span>
            </div>
            <span class="text-[11px] text-slate-400 font-mono">${a.occurred_at}</span>
          </div>

          ${a.project_name ? `
            <div class="text-[11px] font-semibold text-slate-700 bg-white p-2 rounded-lg border border-slate-200/60 flex items-center justify-between">
              <span><i class="fa-solid fa-folder-closed text-amber-500 mr-1.5"></i> Dự án liên quan: <b>${a.project_name}</b> (${a.project_code || ''})</span>
              <span class="font-black text-slate-800">${formatVND(a.project_contract_value)}</span>
            </div>
          ` : ''}

          <p class="text-xs text-slate-600 leading-relaxed">${a.content || ''}</p>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 p-2 bg-white rounded-lg border border-slate-100 text-[11px]">
            <div>
              <span class="text-slate-400">Chi phí tiếp khách:</span>
              <b class="text-rose-600 ml-1 font-mono">${formatVND(a.cost || 0)}</b>
            </div>
            <div class="text-left sm:text-right">
              <span class="text-slate-400">Thẩm quyền duyệt:</span>
              <b class="text-purple-900 ml-1">${a.approver_authority || 'PTGĐ phụ trách SBU'}</b>
            </div>
          </div>

          <div class="flex items-center justify-between text-[11px] pt-1 text-slate-500 border-t border-slate-200/60">
            <div>Đối tác: <b class="text-slate-800">${a.customer_name}</b> (${a.key_decision_maker || ''})</div>
            <div class="text-blue-700 font-bold">Lãnh đạo tham gia: ${a.leader_in_charge}</div>
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    console.error("Error loading care activities:", err);
  }
}

function openNewCareModal() {
  const custSelect = document.getElementById('care-customer');
  if (custSelect) {
    custSelect.innerHTML = allCustomers.map(c => `<option value="${c.id}" data-sbu="${c.sbu}" data-tier="${c.tier}">${c.name} (${c.key_decision_maker || ''})</option>`).join('');
  }
  document.getElementById('care-date').value = new Date().toISOString().split('T')[0];
  document.getElementById('care-leader').value = currentUser.full_name;
  onCareCustomerChange();
  openModal('modal-care');
}

function onCareCustomerChange() {
  const custSelect = document.getElementById('care-customer');
  const projSelect = document.getElementById('care-project');
  if (!custSelect || !projSelect) return;

  const custId = parseInt(custSelect.value, 10);
  let opts = '<option value="">-- Tiếp khách / CSKH Chung (Theo Hạng Đối Tác) --</option>';

  // Link to Bids (gói thầu đang theo đuổi)
  if (window.allBidsList && window.allBidsList.length > 0) {
    const relevantBids = window.allBidsList.filter(b => b.customer_id === custId);
    if (relevantBids.length > 0) {
      opts += '<optgroup label="Gói thầu / Cơ hội đang theo đuổi của CĐT">';
      relevantBids.forEach(b => {
        const cls = classifyFeconProject(b.estimated_value);
        opts += `<option value="BID_${b.id}" data-val="${b.estimated_value}" data-level="${cls.level}" data-approver="${cls.approver_authority}">[${cls.badge}] ${b.project_title} - ${formatVND(b.estimated_value)}</option>`;
      });
      opts += '</optgroup>';
    }

    const otherBids = window.allBidsList.filter(b => b.customer_id !== custId);
    if (otherBids.length > 0) {
      opts += '<optgroup label="Các gói thầu khác">';
      otherBids.forEach(b => {
        const cls = classifyFeconProject(b.estimated_value);
        opts += `<option value="BID_${b.id}" data-val="${b.estimated_value}" data-level="${cls.level}" data-approver="${cls.approver_authority}">[${cls.badge}] ${b.project_title} (${b.sbu})</option>`;
      });
      opts += '</optgroup>';
    }
  }

  // Fallback to allProjects if any exist
  if (allProjects && allProjects.length > 0) {
    const relevantProjects = allProjects.filter(p => p.customer_id === custId);
    if (relevantProjects.length > 0) {
      opts += '<optgroup label="Hồ sơ liên quan khác">';
      relevantProjects.forEach(p => {
        const cls = classifyFeconProject(p.contract_value);
        opts += `<option value="${p.id}" data-val="${p.contract_value}" data-level="${cls.level}" data-approver="${cls.approver_authority}">[${cls.badge}] ${p.name} - ${formatVND(p.contract_value)}</option>`;
      });
      opts += '</optgroup>';
    }
  }

  projSelect.innerHTML = opts;
  onCareProjectChange();
}

function onCareProjectChange() {
  const projSelect = document.getElementById('care-project');
  const custSelect = document.getElementById('care-customer');
  const badgeEl = document.getElementById('care-level-badge');
  const approverEl = document.getElementById('care-approver-badge');
  if (!badgeEl || !approverEl) return;

  const selectedOpt = projSelect?.options[projSelect?.selectedIndex];
  const approverAttr = selectedOpt?.getAttribute('data-approver');
  const valAttr = selectedOpt?.getAttribute('data-val');

  if (approverAttr && valAttr) {
    const cls = classifyFeconProject(parseFloat(valAttr) || 0);
    badgeEl.textContent = cls.badge;
    badgeEl.className = `px-2 py-0.5 rounded-full font-bold border ${cls.colorClass}`;
    approverEl.textContent = approverAttr;
    return;
  }

  const projId = projSelect?.value;
  if (projId && !projId.startsWith('BID_')) {
    const p = allProjects.find(x => x.id === parseInt(projId, 10));
    if (p) {
      const cls = classifyFeconProject(p.contract_value);
      badgeEl.textContent = cls.badge;
      badgeEl.className = `px-2 py-0.5 rounded-full font-bold border ${cls.colorClass}`;
      approverEl.textContent = cls.approver_authority;
      return;
    }
  }

  // Fallback to customer tier policy
  const custOpt = custSelect?.options[custSelect?.selectedIndex];
  const tier = custOpt?.getAttribute('data-tier') || 'GOLD';
  if (tier === 'DIAMOND') {
    badgeEl.textContent = '💎 Kim Cương';
    badgeEl.className = 'px-2 py-0.5 rounded-full font-bold bg-purple-100 text-purple-800 border border-purple-200';
    approverEl.textContent = 'Chủ tịch HĐQT / TGĐ trực tiếp duyệt';
  } else if (tier === 'GOLD') {
    badgeEl.textContent = '🥇 Hạng Vàng';
    badgeEl.className = 'px-2 py-0.5 rounded-full font-bold bg-amber-100 text-amber-900 border border-amber-200';
    approverEl.textContent = 'TGĐ / SBU Leader phụ trách phê duyệt';
  } else {
    badgeEl.textContent = '🥈 Hạng Bạc';
    badgeEl.className = 'px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-800 border border-slate-200';
    approverEl.textContent = 'SBU Leader / GĐKD phụ trách phê duyệt';
  }
}

async function handleCareSubmit(e) {
  e.preventDefault();
  const custSelect = document.getElementById('care-customer');
  const custId = parseInt(custSelect.value);
  const custOpt = custSelect.options[custSelect.selectedIndex];
  const sbu = custOpt ? custOpt.getAttribute('data-sbu') : (currentSBU !== 'ALL' ? currentSBU : 'SBU1');
  const projSelect = document.getElementById('care-project');
  const projId = projSelect && projSelect.value ? parseInt(projSelect.value, 10) : null;

  const payload = {
    customer_id: custId,
    project_id: projId,
    sbu: sbu,
    activity_type: document.getElementById('care-type').value,
    occurred_at: document.getElementById('care-date').value,
    title: document.getElementById('care-title').value,
    leader_in_charge: document.getElementById('care-leader').value,
    content: document.getElementById('care-content').value,
    cost: parseFloat(document.getElementById('care-cost')?.value || 0),
    outcome_status: 'SUCCESS'
  };

  try {
    const res = await authFetch('/api/care-activities', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast("Đã lưu lịch chăm sóc khách hàng thành công!");
      closeModal('modal-care');
      loadCareActivities();
      loadCustomers();
      loadDashboard();
    }
  } catch (err) {
    showToast("Lỗi khi lưu", "error");
  }
}

// ==========================================
// 7. EXECUTIVE MESSAGING & AUTOMATION LOGIC
// ==========================================
function populateCustomerSelects() {
  const msgCustSelect = document.getElementById('msg-customer-select');
  const msgProjSelect = document.getElementById('msg-project-select');

  if (msgCustSelect && allCustomers.length > 0) {
    msgCustSelect.innerHTML = allCustomers.map(c => `
      <option value="${c.id}" data-sbu="${c.sbu}" data-name="${c.name}" data-person="${c.key_decision_maker}" data-role="${c.decision_maker_role || ''}" data-phone="${c.decision_maker_phone || c.phone || ''}">
        ${c.name} - ${c.key_decision_maker} (${c.sbu})
      </option>
    `).join('');
  }

  if (msgProjSelect && allProjects.length > 0) {
    msgProjSelect.innerHTML = '<option value="">-- Không gắn dự án cụ thể --</option>' + 
      allProjects.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} (${p.sbu})</option>`).join('');
  }

  onMessageCustomerChange();
}

function onMessageCustomerChange() {
  const custSelect = document.getElementById('msg-customer-select');
  const opt = custSelect?.options[custSelect.selectedIndex];
  const phone = opt ? opt.getAttribute('data-phone') : '';
  const recipientDisplay = document.getElementById('msg-recipient-display');
  if (recipientDisplay) recipientDisplay.innerText = phone || 'Chưa có SĐT';

  applyTemplateToComposer();
}

function applyTemplateToComposer() {
  const tpl = document.getElementById('msg-template-select')?.value || 'CHUC_MUNG_SINH_NHAT';
  const custSelect = document.getElementById('msg-customer-select');
  const projSelect = document.getElementById('msg-project-select');

  const custOpt = custSelect?.options[custSelect.selectedIndex];
  const projOpt = projSelect?.options[projSelect.selectedIndex];

  const custName = custOpt?.getAttribute('data-name') || "Quý Tập Đoàn";
  const person = custOpt?.getAttribute('data-person') || "Lãnh Đạo Đối Tác";
  const role = custOpt?.getAttribute('data-role') || "Chủ tịch / Tổng Giám Đốc";
  const projName = projOpt?.getAttribute('data-name') || "Dự án trọng điểm";

  let title = "";
  let body = "";

  switch (tpl) {
    case 'CHUC_MUNG_SINH_NHAT':
      title = `Chúc mừng Sinh nhật ${person}`;
      body = `Kính gửi ${person} (${role} - ${custName}): Nhân dịp ngày sinh nhật, Ban Lãnh Đạo Công ty Xây dựng xin trân trọng kính chúc Anh/Chị tuổi mới ngập tràn niềm vui, dồi dào sức khỏe, dẫn dắt Tập đoàn gặt hái thêm nhiều thắng lợi mới và tiếp tục gắn bó bền chặt cùng chúng tôi!`;
      break;
    case 'THANH_LAP_DOI_TAC':
      title = `Chúc mừng Ngày truyền thống / Thành lập ${custName}`;
      body = `Ban Lãnh Đạo Công ty Xây dựng trân trọng chúc mừng ${custName} nhân dịp kỷ niệm ngày truyền thống. Kính chúc Quý Tập đoàn ngày càng lớn mạnh, tiếp tục khẳng định vị thế dẫn đầu và đồng hành cùng chúng tôi kiến tạo các công trình tầm vóc quốc gia!`;
      break;
    case 'TIEN_DO_LANH_DAO':
      title = `Báo cáo tiến độ điều hành: ${projName}`;
      body = `Kính gửi ${person} (${custName}): Ban Lãnh Đạo Công ty Xây dựng trân trọng báo cáo: Hạng mục trọng điểm thuộc dự án ${projName} đã vượt mốc tiến độ đề ra, bảo đảm an toàn và chất lượng tuyệt đối theo đúng cam kết.`;
      break;
    case 'THONG_BAO_NGHIEM_THU':
      title = `Thông báo nghiệm thu mốc hoàn thành: ${projName}`;
      body = `Kính gửi ${person}: Công tác nghiệm thu kỹ thuật mốc đợt này thuộc công trình ${projName} đã hoàn tất đạt chuẩn. Kính đề nghị Quý Lãnh đạo hỗ trợ phê duyệt giải ngân theo điều khoản hợp đồng. Trân trọng cảm ơn!`;
      break;
  }

  const titleInput = document.getElementById('msg-title-input');
  const bodyInput = document.getElementById('msg-body-input');
  if (titleInput) titleInput.value = title;
  if (bodyInput) bodyInput.value = body;

  updateLivePreview();
}

function updateLivePreview() {
  const title = document.getElementById('msg-title-input')?.value || "";
  const body = document.getElementById('msg-body-input')?.value || "";
  const previewTitle = document.getElementById('preview-msg-title');
  const previewBody = document.getElementById('preview-msg-body');

  if (previewTitle) previewTitle.innerText = title;
  if (previewBody) previewBody.innerText = body;
}

async function submitSendMessage() {
  const custSelect = document.getElementById('msg-customer-select');
  const projSelect = document.getElementById('msg-project-select');
  const channel = document.querySelector('input[name="msg-channel"]:checked')?.value || "ZALO_ZNS";
  const templateType = document.getElementById('msg-template-select')?.value;
  const title = document.getElementById('msg-title-input')?.value;
  const messageBody = document.getElementById('msg-body-input')?.value;
  const recipient = document.getElementById('msg-recipient-display')?.innerText;

  const custOpt = custSelect?.options[custSelect.selectedIndex];
  const sbu = custOpt ? custOpt.getAttribute('data-sbu') : 'SBU1';

  if (!custSelect?.value || !messageBody) {
    showToast("Vui lòng nhập nội dung tin nhắn", "error");
    return;
  }

  const payload = {
    customer_id: parseInt(custSelect.value),
    project_id: projSelect?.value ? parseInt(projSelect.value) : null,
    sbu: sbu,
    channel: channel,
    template_type: templateType,
    recipient: recipient,
    title: title,
    message_body: messageBody
  };

  try {
    const res = await authFetch('/api/notifications/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast(`Đã gửi tin nhắn ngoại giao qua kênh ${channel} thành công!`);
    } else {
      showToast("Lỗi khi gửi tin nhắn", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối", "error");
  }
}

async function triggerAutomatedScan() {
  showToast("Đang quét lịch sinh nhật và ngày thành lập đối tác...", "info");
  try {
    const res = await authFetch('/api/notifications/run-auto', { method: 'POST' });
    const data = await res.json();
    showToast(data.message || "Quét thành công!");
    loadDashboard();
  } catch (err) {
    showToast("Lỗi khi quét tự động", "error");
  }
}

function prepareZaloGreeting(custId, personName, phone, custName) {
  switchTab('care');
  const custSelect = document.getElementById('msg-customer-select');
  if (custSelect) {
    custSelect.value = custId;
    onMessageCustomerChange();
  }
}

function quickMessageToLeader(custId) {
  switchTab('care');
  const custSelect = document.getElementById('msg-customer-select');
  if (custSelect) {
    custSelect.value = custId;
    onMessageCustomerChange();
  }
}

// ==========================================
// MOBILE APP INSTALLATION MODAL & iOS PROMPTS
// ==========================================
function openAppInstallModal(defaultTab = 'android') {
  switchInstallTab(defaultTab);
  const urlInput = document.getElementById('ios-web-url-input');
  if (urlInput) urlInput.value = window.location.origin;
  openModal('modal-app-install');
}

function switchInstallTab(tab) {
  const tabAndroid = document.getElementById('tab-btn-android');
  const tabIos = document.getElementById('tab-btn-ios');
  const contentAndroid = document.getElementById('tab-content-android');
  const contentIos = document.getElementById('tab-content-ios');

  if (tab === 'ios') {
    if (tabIos) {
      tabIos.className = 'flex-1 py-2 px-3 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 bg-white text-[#0A3583] shadow-sm cursor-pointer';
    }
    if (tabAndroid) {
      tabAndroid.className = 'flex-1 py-2 px-3 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 text-slate-600 hover:text-slate-900 cursor-pointer';
    }
    if (contentIos) contentIos.classList.remove('hidden');
    if (contentAndroid) contentAndroid.classList.add('hidden');
  } else {
    if (tabAndroid) {
      tabAndroid.className = 'flex-1 py-2 px-3 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 bg-white text-emerald-700 shadow-sm cursor-pointer';
    }
    if (tabIos) {
      tabIos.className = 'flex-1 py-2 px-3 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 text-slate-600 hover:text-slate-900 cursor-pointer';
    }
    if (contentAndroid) contentAndroid.classList.remove('hidden');
    if (contentIos) contentIos.classList.add('hidden');
  }
}

function copyWebLink() {
  const url = window.location.origin;
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(url).then(() => {
      showToast('Đã sao chép link web! Hãy mở Safari trên iPhone và dán để cài đặt.', 'success');
    }).catch(() => {
      showToast('Địa chỉ web: ' + url, 'info');
    });
  } else {
    showToast('Địa chỉ web: ' + url, 'info');
  }
}

function checkIosDeviceAndShowBanner() {
  const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
  const isStandalone = window.navigator.standalone === true || window.matchMedia('(display-mode: standalone)').matches;
  const isDismissed = sessionStorage.getItem('fecon_ios_banner_dismissed') === 'true';

  if (isIos && !isStandalone && !isDismissed) {
    const banner = document.getElementById('ios-pwa-install-banner');
    if (banner) {
      setTimeout(() => {
        banner.classList.remove('hidden');
      }, 1200);
    }
  }
}

function dismissIosBanner() {
  const banner = document.getElementById('ios-pwa-install-banner');
  if (banner) banner.classList.add('hidden');
  sessionStorage.setItem('fecon_ios_banner_dismissed', 'true');
}

