package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.DashboardStats
import com.example.crmxaydung.data.ProjectItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun DashboardScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSbu by remember { mutableStateOf(if (user.role == "ADMIN") "ALL" else user.sbu) }
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val sbuList = listOf(
        "ALL" to "Tất Cả SBU",
        "SBU1" to "SBU1 - Móng & Hầm",
        "SBU2" to "SBU2 - Năng Lượng",
        "SBU3" to "SBU3 - Metro Ngầm",
        "SBU4" to "SBU4 - Đường Sắt",
        "SBU5" to "SBU5 - Cảng Biển"
    )

    fun loadData(sbu: String) {
        isLoading = true
        coroutineScope.launch {
            val statsRes = ApiClient.fetchDashboardStats(sbu)
            val projRes = ApiClient.fetchProjects(sbu)
            stats = statsRes.getOrNull() ?: DashboardStats()
            projects = projRes.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(selectedSbu) {
        loadData(selectedSbu)
    }

    val df = DecimalFormat("#,##0.0")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Active User Welcome Banner (Compact White Card)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Xin chào, ${user.fullName}",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${user.title} | ${user.role} (${user.sbu})",
                        color = Color(0xFFEA580C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SBU Filter (Only for Admin)
        if (user.role == "ADMIN") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Khối SBU:",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sbuList) { (key, label) ->
                        val isSelected = (selectedSbu == key)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSbu = key },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEA580C),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFEA580C))
            }
        } else {
            val s = stats ?: DashboardStats()

            // 4 KPI Cards Grid (Focusing on Strategic Customers & Bidding Funnel)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KpiCard(
                    title = "Khách Hàng",
                    value = "${s.totalCustomers}",
                    sub = "Đối tác chiến lược",
                    bgColor = Color(0xFFEFF6FF),
                    borderColor = Color(0xFFBFDBFE),
                    titleColor = Color(0xFF1E40AF),
                    valColor = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Gói Thầu Đang Đấu",
                    value = "${s.activeBidsCount}",
                    sub = "${df.format(s.activeBidsBillion)} tỷ trong phễu",
                    bgColor = Color(0xFFFFF7ED),
                    borderColor = Color(0xFFFED7AA),
                    titleColor = Color(0xFF9A3412),
                    valColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KpiCard(
                    title = "Gói Thầu Đã Trúng",
                    value = "${s.wonBidsCount}",
                    sub = "${df.format(s.wonBidsBillion)} tỷ thành công",
                    bgColor = Color(0xFFECFDF5),
                    borderColor = Color(0xFFA7F3D0),
                    titleColor = Color(0xFF065F46),
                    valColor = Color(0xFF047857),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Giá Trị Hợp Đồng",
                    value = "${df.format(s.totalContractBillion)} tỷ",
                    sub = "Đã thu: ${df.format(s.totalCollectedBillion)} tỷ",
                    bgColor = Color(0xFFFAF5FF),
                    borderColor = Color(0xFFE9D5FF),
                    titleColor = Color(0xFF6B21A8),
                    valColor = Color(0xFF581C87),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Projects Highlight Section
            Text(
                text = "🏗️ Tiến Độ Các Dự Án Trọng Điểm",
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (projects.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                ) {
                    Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có dự án trong phân hệ này.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            } else {
                projects.take(5).forEach { proj ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = proj.name,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEA580C),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        proj.sbu,
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "CĐT: ${proj.customerName} | HĐ: ${df.format(proj.contractValueBillion)} tỷ",
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { (proj.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFFE2E8F0),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${proj.progressPercent.toInt()}%",
                                    color = Color(0xFF059669),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    sub: String,
    bgColor: Color,
    borderColor: Color,
    titleColor: Color,
    valColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(title, color = titleColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = valColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(1.dp))
            Text(sub, color = titleColor.copy(alpha = 0.8f), fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}
