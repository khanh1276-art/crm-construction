package com.example.crmxaydung.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    var selectedSbu by remember { mutableStateOf(if (user.role == "ADMIN") "ALL" else user.sbu) }
    var showSbuMenu by remember { mutableStateOf(false) }

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

                    // Right: Khối SBU Dropdown (với tickbox) + Tên tài khoản + Nút Đăng xuất
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Khối SBU Dropdown with tickbox
                        Box {
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { showSbuMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedSbu == "ALL") "Khối: Tất cả" else selectedSbu,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("▾", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            DropdownMenu(
                                expanded = showSbuMenu,
                                onDismissRequest = { showSbuMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                sbuOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = (selectedSbu == key),
                                                    onCheckedChange = {
                                                        selectedSbu = key
                                                        showSbuMenu = false
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = feconDeepOrange)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selectedSbu == key) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedSbu = key
                                            showSbuMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // User Account Name next to Logout
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = user.fullName.split(" ").lastOrNull() ?: user.fullName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Compact Logout Button
                        Surface(
                            color = Color(0xFFB91C1C).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    ApiClient.currentUser = null
                                    onLogout()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("🚪", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Thoát", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
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
                            onClick = { currentTab = NavTab.PIPELINE },
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
                NavTab.DASHBOARD -> DashboardScreen(user = user, selectedSbu = selectedSbu)
                NavTab.CUSTOMERS -> CustomerScreen(user = user, selectedSbu = selectedSbu)
                NavTab.PIPELINE -> ProjectScreen(user = user, selectedSbu = selectedSbu)
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
