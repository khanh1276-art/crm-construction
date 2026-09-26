package com.example.crmxaydung.ui

import androidx.compose.foundation.background
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
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Active User Welcome Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Xin chào, ${user.fullName}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${user.title} | ${user.role} (${user.sbu})",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SBU Filter (Only for Admin, or shows locked tag for GĐKD)
        if (user.role == "ADMIN") {
            Text("Phân bổ Khối SBU:", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sbuList) { (key, label) ->
                    FilterChip(
                        selected = (selectedSbu == key),
                        onClick = { selectedSbu = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            val s = stats ?: DashboardStats()

            // 4 KPI Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Khách Hàng",
                    value = "${s.totalCustomers}",
                    sub = "${s.strategicVipCount} đối tác VIP",
                    bgColor = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Dự Án Đang Chạy",
                    value = "${s.totalProjects}",
                    sub = "Đang theo dõi",
                    bgColor = Color(0xFF065F46),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Giá Trị Hợp Đồng",
                    value = "${df.format(s.totalContractBillion)} tỷ",
                    sub = "Đã thu: ${df.format(s.totalCollectedBillion)} tỷ",
                    bgColor = Color(0xFF581C87),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Tiến Độ Trung Bình",
                    value = "${df.format(s.avgProgress)}%",
                    sub = "Tiến độ toàn khối",
                    bgColor = Color(0xFF9A3412),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Projects Highlight Section
            Text(
                text = "🏗️ Tiến Độ Các Dự Án Trọng Điểm",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (projects.isEmpty()) {
                Text("Chưa có dự án trong phân hệ này.", color = Color.Gray, fontSize = 13.sp)
            } else {
                projects.take(5).forEach { proj ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(proj.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = Color(0xFF0369A1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        proj.sbu,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CĐT: ${proj.customerName} | HĐ: ${df.format(proj.contractValueBillion)} tỷ",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { (proj.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFF334155),
                                    modifier = Modifier.weight(1f).height(6.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${proj.progressPercent.toInt()}%",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
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
fun KpiCard(title: String, value: String, sub: String, bgColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = Color(0xFFCBD5E1), fontSize = 10.sp)
        }
    }
}
