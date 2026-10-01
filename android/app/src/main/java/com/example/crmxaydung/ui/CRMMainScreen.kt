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

    // FECON Brand Palette
    val feconDeepOrange = Color(0xFFEA580C)
    val feconLightCream = Color(0xFFFEF3C7)
    val lightBg = Color(0xFFF8FAFC)
    val dividerColor = Color(0xFFE2E8F0)

    Scaffold(
        topBar = {
            Surface(
                color = feconDeepOrange,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo + App Name + User Identity on 1 compact row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.fecon_crm_logo),
                                contentDescription = "FECON CRM",
                                modifier = Modifier
                                    .height(26.dp)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column {
                            Text(
                                text = "FECON CRM",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "${user.fullName} • ${user.sbu}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = feconLightCream,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Compact Logout Button
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                ApiClient.currentUser = null
                                onLogout()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🚪 Đăng xuất", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                NavTab.DASHBOARD -> DashboardScreen(user = user)
                NavTab.CUSTOMERS -> CustomerScreen(user = user)
                NavTab.PIPELINE -> ProjectScreen(user = user)
                NavTab.CARE -> CareScreen(user = user)
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
