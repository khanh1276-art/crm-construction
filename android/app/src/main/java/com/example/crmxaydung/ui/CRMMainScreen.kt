package com.example.crmxaydung.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    // Light Orange Color Palette for Toolbars
    val lightOrangeBg = Color(0xFFFFF2E6)
    val lightOrangeBorder = Color(0xFFFED7AA)
    val brandOrange = Color(0xFFEA580C)
    val brandDarkOrange = Color(0xFF9A3412)
    val tabUnselectedColor = Color(0xFF78716C)
    val tabSelectedIndicator = Color(0xFFFFD7BA)

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
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = brandDarkOrange
                                )
                                Text(
                                    text = "${user.fullName} (${user.sbu})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = brandOrange
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                ApiClient.currentUser = null
                                onLogout()
                            }
                        ) {
                            Text("🚪 Đăng xuất", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = lightOrangeBg
                    )
                )
                // Subtle divider below top app bar
                HorizontalDivider(color = lightOrangeBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = lightOrangeBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = lightOrangeBg,
                    contentColor = brandDarkOrange
                ) {
                    NavigationBarItem(
                        selected = (currentTab == NavTab.DASHBOARD),
                        onClick = { currentTab = NavTab.DASHBOARD },
                        icon = { Text("📊", fontSize = 16.sp) },
                        label = { Text("Tổng Quan", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = brandOrange,
                            selectedIconColor = brandOrange,
                            indicatorColor = tabSelectedIndicator,
                            unselectedTextColor = tabUnselectedColor,
                            unselectedIconColor = tabUnselectedColor
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.CUSTOMERS),
                        onClick = { currentTab = NavTab.CUSTOMERS },
                        icon = { Text("🏢", fontSize = 16.sp) },
                        label = { Text("Khách Hàng", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.CUSTOMERS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = brandOrange,
                            selectedIconColor = brandOrange,
                            indicatorColor = tabSelectedIndicator,
                            unselectedTextColor = tabUnselectedColor,
                            unselectedIconColor = tabUnselectedColor
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.PROJECTS),
                        onClick = { currentTab = NavTab.PROJECTS },
                        icon = { Text("🏗️", fontSize = 16.sp) },
                        label = { Text("Dự Án", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.PROJECTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = brandOrange,
                            selectedIconColor = brandOrange,
                            indicatorColor = tabSelectedIndicator,
                            unselectedTextColor = tabUnselectedColor,
                            unselectedIconColor = tabUnselectedColor
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.CARE),
                        onClick = { currentTab = NavTab.CARE },
                        icon = { Text("📅", fontSize = 16.sp) },
                        label = { Text("Lịch Chăm Sóc", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.CARE) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = brandOrange,
                            selectedIconColor = brandOrange,
                            indicatorColor = tabSelectedIndicator,
                            unselectedTextColor = tabUnselectedColor,
                            unselectedIconColor = tabUnselectedColor
                        )
                    )
                    NavigationBarItem(
                        selected = (currentTab == NavTab.ACCOUNTS),
                        onClick = { currentTab = NavTab.ACCOUNTS },
                        icon = { Text("👥", fontSize = 16.sp) },
                        label = { Text("Tài Khoản", fontSize = 10.sp, fontWeight = if (currentTab == NavTab.ACCOUNTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = brandOrange,
                            selectedIconColor = brandOrange,
                            indicatorColor = tabSelectedIndicator,
                            unselectedTextColor = tabUnselectedColor,
                            unselectedIconColor = tabUnselectedColor
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
