package com.example.crmxaydung.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.R
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserSession

enum class NavTab {
    DASHBOARD, CUSTOMERS, PROJECTS, CARE, ACCOUNTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CRMMainScreen(
    user: UserSession,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    // FECON Brand Palette
    val feconDeepOrange = Color(0xFFEA580C) // Vàng cam đậm thương hiệu FECON
    val feconLightCream = Color(0xFFFEF3C7) // Màu kem nhạt cho phụ đề trên thanh cam
    val lightBg = Color(0xFFF8FAFC)         // Nền ứng dụng sáng (Slate 50)
    val dividerColor = Color(0xFFE2E8F0)    // Viền nhẹ
    val navSelectedIndicator = Color(0xFFFFEDD5) // Cam rất nhạt cho icon được chọn
    val navUnselectedText = Color(0xFF64748B)    // Xám cho icon không chọn

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.fecon_crm_logo),
                                contentDescription = "FECON CRM",
                                modifier = Modifier
                                    .height(38.dp)
                                    .padding(end = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    text = "FECON CRM",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "${user.fullName} • ${user.sbu}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = feconLightCream
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    ApiClient.currentUser = null
                                    onLogout()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🚪 Đăng xuất", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = feconDeepOrange
                    )
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = feconDeepOrange,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = (currentTab == NavTab.DASHBOARD),
                        onClick = { currentTab = NavTab.DASHBOARD },
                        icon = { Text("📊", fontSize = 16.sp) },
                        label = { Text("Tổng Quan", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = feconDeepOrange,
                            selectedIconColor = feconDeepOrange,
                            indicatorColor = navSelectedIndicator,
                            unselectedTextColor = navUnselectedText,
                            unselectedIconColor = navUnselectedText
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.CUSTOMERS),
                        onClick = { currentTab = NavTab.CUSTOMERS },
                        icon = { Text("🏢", fontSize = 16.sp) },
                        label = { Text("Khách Hàng", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.CUSTOMERS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = feconDeepOrange,
                            selectedIconColor = feconDeepOrange,
                            indicatorColor = navSelectedIndicator,
                            unselectedTextColor = navUnselectedText,
                            unselectedIconColor = navUnselectedText
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.PROJECTS),
                        onClick = { currentTab = NavTab.PROJECTS },
                        icon = { Text("🏗️", fontSize = 16.sp) },
                        label = { Text("Dự Án", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.PROJECTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = feconDeepOrange,
                            selectedIconColor = feconDeepOrange,
                            indicatorColor = navSelectedIndicator,
                            unselectedTextColor = navUnselectedText,
                            unselectedIconColor = navUnselectedText
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.CARE),
                        onClick = { currentTab = NavTab.CARE },
                        icon = { Text("📅", fontSize = 16.sp) },
                        label = { Text("Lịch Chăm Sóc", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.CARE) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = feconDeepOrange,
                            selectedIconColor = feconDeepOrange,
                            indicatorColor = navSelectedIndicator,
                            unselectedTextColor = navUnselectedText,
                            unselectedIconColor = navUnselectedText
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.ACCOUNTS),
                        onClick = { currentTab = NavTab.ACCOUNTS },
                        icon = { Text("👥", fontSize = 16.sp) },
                        label = { Text("Tài Khoản", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.ACCOUNTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = feconDeepOrange,
                            selectedIconColor = feconDeepOrange,
                            indicatorColor = navSelectedIndicator,
                            unselectedTextColor = navUnselectedText,
                            unselectedIconColor = navUnselectedText
                        )
                    )
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
                NavTab.PROJECTS -> ProjectScreen(user = user)
                NavTab.CARE -> CareScreen(user = user)
                NavTab.ACCOUNTS -> UserManagementScreen(currentUser = user)
            }
        }
    }
}
