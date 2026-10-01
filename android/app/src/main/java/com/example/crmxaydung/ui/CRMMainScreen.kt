package com.example.crmxaydung.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.R
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserSession

enum class NavTab {
    DASHBOARD, CUSTOMERS, PIPELINE, CARE, ACCOUNTS
}

@Composable
fun CRMMainScreen(
    user: UserSession,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var targetPipelineStage by remember { mutableStateOf("ALL") }
    var selectedSbu by remember { mutableStateOf(if (user.role == "ADMIN") "ALL" else user.sbu) }
    var showUserMenu by remember { mutableStateOf(false) }

    val sbuOptions = listOf(
        "ALL" to "Tất cả SBU",
        "SBU1" to "SBU1 - Móng & Hầm",
        "SBU2" to "SBU2 - Năng Lượng",
        "SBU3" to "SBU3 - Metro Ngầm",
        "SBU4" to "SBU4 - Đường Sắt",
        "SBU5" to "SBU5 - Cảng Biển"
    )

    // FECON Brand Palette
    val feconDeepOrange = Color(0xFFEA580C)
    val feconLightCream = Color(0xFFFEF3C7)
    val lightBg = Color(0xFFF8FAFC)
    val dividerColor = Color(0xFFE2E8F0)

    Scaffold(
        topBar = {
            Surface(
                color = feconDeepOrange,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Logo FECON
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(7.dp),
                        shadowElevation = 1.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fecon_crm_logo),
                            contentDescription = "FECON Logo",
                            modifier = Modifier
                                .height(32.dp)
                                .padding(horizontal = 5.dp, vertical = 2.5.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Center: Tên phần mềm FECON - Quản lý và chăm sóc khách hàng
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "FECON",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Quản lý và chăm sóc khách hàng",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Right: Gộp Tên người sử dụng (bỏ chức danh) và biểu tượng thoát, nằm đối xứng với logo FECON
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            // Tên người sử dụng (chỉ để tên, bỏ chức danh)
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showUserMenu = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("👤", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = user.fullName,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 115.dp)
                                    )
                                    if (user.role == "ADMIN") {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("▾", color = Color.White, fontSize = 9.sp)
                                    }
                                }

                                // Popup menu khi bấm vào tên người dùng
                                DropdownMenu(
                                    expanded = showUserMenu,
                                    onDismissRequest = { showUserMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = user.fullName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                if (user.title.isNotEmpty()) {
                                                    Text(
                                                        text = user.title,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                Text(
                                                    text = "Vai trò: ${user.role}",
                                                    fontSize = 9.5.sp,
                                                    color = feconDeepOrange,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        onClick = { showUserMenu = false }
                                    )

                                    if (user.role == "ADMIN") {
                                        HorizontalDivider(color = dividerColor)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "🏢 Đổi Khối SBU:",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = feconDeepOrange
                                                )
                                            },
                                            onClick = {}
                                        )
                                        sbuOptions.forEach { (key, label) ->
                                            val isSelected = (selectedSbu == key)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) feconDeepOrange else Color(0xFF0F172A)
                                                        )
                                                        if (isSelected) {
                                                            Text("✓", color = feconDeepOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedSbu = key
                                                    showUserMenu = false
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = dividerColor)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🚪", fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Đăng xuất",
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            showUserMenu = false
                                            ApiClient.currentUser = null
                                            onLogout()
                                        }
                                    )
                                }
                            }

                            // Vạch phân cách mảnh giữa Tên và Biểu tượng thoát
                            Box(
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(1.dp)
                                    .background(Color.White.copy(alpha = 0.45f))
                            )

                            // Biểu tượng thoát (Thoát/Đăng xuất nhanh)
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        ApiClient.currentUser = null
                                        onLogout()
                                    }
                                    .padding(start = 5.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = "🚪",
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column {
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactBottomNavItem(
                            selected = (currentTab == NavTab.DASHBOARD),
                            onClick = { currentTab = NavTab.DASHBOARD },
                            icon = "📊",
                            label = "Tổng Quan",
                            activeColor = feconDeepOrange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactBottomNavItem(
                            selected = (currentTab == NavTab.CUSTOMERS),
                            onClick = { currentTab = NavTab.CUSTOMERS },
                            icon = "🏢",
                            label = "Khách Hàng",
                            activeColor = feconDeepOrange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactBottomNavItem(
                            selected = (currentTab == NavTab.PIPELINE),
                            onClick = {
                                targetPipelineStage = "ALL"
                                currentTab = NavTab.PIPELINE
                            },
                            icon = "🎯",
                            label = "Phễu Thầu",
                            activeColor = feconDeepOrange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactBottomNavItem(
                            selected = (currentTab == NavTab.CARE),
                            onClick = { currentTab = NavTab.CARE },
                            icon = "📅",
                            label = "Lịch CSKH",
                            activeColor = feconDeepOrange,
                            modifier = Modifier.weight(1f)
                        )
                        if (user.role == "ADMIN") {
                            CompactBottomNavItem(
                                selected = (currentTab == NavTab.ACCOUNTS),
                                onClick = { currentTab = NavTab.ACCOUNTS },
                                icon = "👥",
                                label = "Tài Khoản",
                                activeColor = feconDeepOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(lightBg)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> DashboardScreen(
                    user = user,
                    selectedSbu = selectedSbu,
                    onSelectSbu = { selectedSbu = it },
                    onNavigateToCustomers = {
                        currentTab = NavTab.CUSTOMERS
                    },
                    onNavigateToActiveBids = {
                        targetPipelineStage = "ACTIVE"
                        currentTab = NavTab.PIPELINE
                    },
                    onNavigateToWonBids = {
                        targetPipelineStage = "WON"
                        currentTab = NavTab.PIPELINE
                    }
                )
                NavTab.CUSTOMERS -> CustomerScreen(user = user, selectedSbu = selectedSbu)
                NavTab.PIPELINE -> ProjectScreen(
                    user = user,
                    selectedSbu = selectedSbu,
                    initialStageFilter = targetPipelineStage
                )
                NavTab.CARE -> CareScreen(user = user, selectedSbu = selectedSbu)
                NavTab.ACCOUNTS -> UserManagementScreen(currentUser = user)
            }
        }
    }
}

@Composable
private fun CompactBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: String,
    label: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            color = if (selected) Color(0xFFFFEDD5) else Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
