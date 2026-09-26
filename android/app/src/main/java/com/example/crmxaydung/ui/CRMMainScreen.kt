package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏗️ CRM LÃNH ĐẠO & 5 SBU", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${user.fullName} (${user.sbu})", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        ApiClient.currentUser = null
                        onLogout()
                    }) {
                        Text("🚪 Đăng xuất", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = (currentTab == NavTab.DASHBOARD),
                    onClick = { currentTab = NavTab.DASHBOARD },
                    icon = { Text("📊", fontSize = 16.sp) },
                    label = { Text("Tổng Quan", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF2563EB),
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == NavTab.CUSTOMERS),
                    onClick = { currentTab = NavTab.CUSTOMERS },
                    icon = { Text("🏢", fontSize = 16.sp) },
                    label = { Text("Khách Hàng", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF2563EB),
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == NavTab.PROJECTS),
                    onClick = { currentTab = NavTab.PROJECTS },
                    icon = { Text("🏗️", fontSize = 16.sp) },
                    label = { Text("Dự Án", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF2563EB),
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == NavTab.CARE),
                    onClick = { currentTab = NavTab.CARE },
                    icon = { Text("🤝", fontSize = 16.sp) },
                    label = { Text("Chăm Sóc", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF2563EB),
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == NavTab.ACCOUNTS),
                    onClick = { currentTab = NavTab.ACCOUNTS },
                    icon = { Text("👥", fontSize = 16.sp) },
                    label = { Text("Tài Khoản", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF2563EB),
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
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
